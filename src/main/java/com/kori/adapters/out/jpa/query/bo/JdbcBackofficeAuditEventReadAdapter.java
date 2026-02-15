package com.kori.adapters.out.jpa.query.bo;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JdbcBackofficeAuditEventReadAdapter implements BackofficeAuditEventReadPort {
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final OpaqueCursorCodec codec = new OpaqueCursorCodec();
    private final ObjectMapper objectMapper;

    public JdbcBackofficeAuditEventReadAdapter(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public QueryPage<BackofficeAuditEventItem> list(BackofficeAuditEventQuery query) {
        int limit = QueryInputValidator.normalizeLimit(query.limit(), DEFAULT_LIMIT, MAX_LIMIT);
        var cursor = codec.decode(query.cursor());
        var sortDesc = resolveSort(query.sort());
        StringBuilder sql = new StringBuilder("SELECT id, occurred_at, actor_type, actor_id, action, metadata_json FROM audit_events WHERE 1=1");
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
            sql.append(" AND actor_type = :resourceType");
            params.addValue("resourceType", query.resourceType());
        }
        if (query.resourceId() != null && !query.resourceId().isBlank()) {
            sql.append(" AND actor_id = :resourceId");
            params.addValue("resourceId", query.resourceId());
        }
        if (query.from() != null) {
            sql.append(" AND occurred_at >= :from");
            params.addValue("from", query.from());
        }
        if (query.to() != null) {
            sql.append(" AND occurred_at <= :to");
            params.addValue("to", query.to());
        }
        if (cursor != null) {
            sql.append(sortDesc
                    ? " AND (occurred_at < :cursorCreatedAt OR (occurred_at = :cursorCreatedAt AND id < CAST(:cursorId AS uuid)))"
                    : " AND (occurred_at > :cursorCreatedAt OR (occurred_at = :cursorCreatedAt AND id > CAST(:cursorId AS uuid)))");
            params.addValue("cursorCreatedAt", cursor.createdAt());
            params.addValue("cursorId", cursor.id());
        }
        sql.append(" ORDER BY occurred_at ").append(sortDesc ? "DESC" : "ASC").append(", id ").append(sortDesc ? "DESC" : "ASC");
        sql.append(" LIMIT :limit");
        params.addValue("limit", limit + 1);

        List<BackofficeAuditEventItem> rows = jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            try {
                return new BackofficeAuditEventItem(
                        rs.getString("id"),
                        rs.getTimestamp("occurred_at").toInstant(),
                        rs.getString("actor_type"),
                        rs.getString("actor_id"),
                        rs.getString("action"),
                        rs.getString("actor_type"),
                        rs.getString("actor_id"),
                        objectMapper.readValue(rs.getString("metadata_json"), new TypeReference<Map<String, Object>>() {})
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        boolean hasMore = rows.size() > limit;
        if (hasMore) rows = new ArrayList<>(rows.subList(0, limit));
        String next = hasMore && !rows.isEmpty() ? codec.encode(rows.get(rows.size() - 1).occurredAt(), UUID.fromString(rows.get(rows.size() - 1).eventId())) : null;
        return new QueryPage<>(rows, next, hasMore);
    }

    private boolean resolveSort(String sortRaw) {
        return QueryInputValidator.resolveSort(sortRaw, "occurredAt");
    }
}
