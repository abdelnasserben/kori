package com.kori.adapters.out.jpa.query.bo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kori.adapters.out.jpa.query.common.OpaqueCursorCodec;
import com.kori.adapters.out.jpa.query.common.QueryInputValidator;
import com.kori.query.model.BackofficeAuditEventItem;
import com.kori.query.model.BackofficeAuditEventQuery;
import com.kori.query.model.QueryPage;
import com.kori.query.port.out.BackofficeAuditEventReadPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class JdbcBackofficeAuditEventReadAdapter implements BackofficeAuditEventReadPort {
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final OpaqueCursorCodec codec = new OpaqueCursorCodec();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JdbcBackofficeAuditEventReadAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public QueryPage<BackofficeAuditEventItem> list(BackofficeAuditEventQuery query) {
        int limit = QueryInputValidator.normalizeLimit(query.limit(), DEFAULT_LIMIT, MAX_LIMIT);
        boolean desc = QueryInputValidator.resolveSort(query.sort(), "occurredAt");
        var cursor = codec.decode(query.cursor());
        StringBuilder sql = new StringBuilder("""
           SELECT id::text AS event_ref, occurred_at, actor_type, actor_id AS actor_ref, action,
               metadata_json::jsonb ->> 'resourceType' AS resource_type,
               metadata_json::jsonb ->> 'resourceRef' AS resource_ref,
               metadata_json
          FROM audit_events
          WHERE 1=1
        """);
        var params = new MapSqlParameterSource();
        if (query.action() != null && !query.action().isBlank()) {
            sql.append(" AND action = :action");
            params.addValue("action", query.action());
        }
        if (query.actorType() != null && !query.actorType().isBlank()) {
            sql.append(" AND actor_type = :actorType");
            params.addValue("actorType", query.actorType());
        }
        if (query.actorRef() != null && !query.actorRef().isBlank()) {
            sql.append(" AND actor_id = :actorRef");
            params.addValue("actorRef", query.actorRef());
        }
        if (query.resourceType() != null && !query.resourceType().isBlank()) {
            sql.append(" AND metadata_json::jsonb ->> 'resourceType' = :resourceType");
            params.addValue("resourceType", query.resourceType());
        }
        if (query.resourceRef() != null && !query.resourceRef().isBlank()) {
            sql.append(" AND metadata_json::jsonb ->> 'resourceRef' = :resourceRef");
            params.addValue("resourceRef", query.resourceRef());
        }
        if (query.from() != null) {
            sql.append(" AND occurred_at >= :from");
            params.addValue("from", Timestamp.from(query.from()));
        }
        if (query.to() != null) {
            sql.append(" AND occurred_at <= :to");
            params.addValue("to", Timestamp.from(query.to()));
        }
        if (cursor != null) {
            sql.append(desc
                    ? " AND (occurred_at < :cursorCreatedAt OR (occurred_at = :cursorCreatedAt AND id::text < :cursorRef))"
                    : " AND (occurred_at > :cursorCreatedAt OR (occurred_at = :cursorCreatedAt AND id::text > :cursorRef))");
            params.addValue("cursorCreatedAt", Timestamp.from(cursor.createdAt()));
            params.addValue("cursorRef", cursor.ref());
        }
        sql.append(" ORDER BY occurred_at ").append(desc ? "DESC" : "ASC").append(", id ").append(desc ? "DESC" : "ASC").append(" LIMIT :limit");
        params.addValue("limit", limit + 1);

        List<BackofficeAuditEventItem> rows = jdbcTemplate.query(sql.toString(), params, (rs, i) -> new BackofficeAuditEventItem(
                rs.getString("event_ref"), rs.getTimestamp("occurred_at").toInstant(), rs.getString("actor_type"), rs.getString("actor_ref"),
                rs.getString("action"), rs.getString("resource_type"), rs.getString("resource_ref"), parseMetadata(rs.getString("metadata_json"))
        ));

        boolean hasMore = rows.size() > limit;
        if (hasMore) rows = new ArrayList<>(rows.subList(0, limit));
        String next = hasMore && !rows.isEmpty()
                ? codec.encode(rows.get(rows.size() - 1).occurredAt(), rows.get(rows.size() - 1).eventRef())
                : null;
        return new QueryPage<>(rows, next, hasMore);
    }

    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<>() {});
        }
        catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }
}
