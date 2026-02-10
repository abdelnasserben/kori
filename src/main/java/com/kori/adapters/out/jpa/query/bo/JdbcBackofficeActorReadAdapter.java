package com.kori.adapters.out.jpa.query.bo;

import com.kori.adapters.out.jpa.query.common.OpaqueCursorCodec;
import com.kori.adapters.out.jpa.query.common.QueryInputValidator;
import com.kori.application.port.out.query.BackofficeActorReadPort;
import com.kori.application.query.BackofficeActorItem;
import com.kori.application.query.BackofficeActorQuery;
import com.kori.application.query.QueryPage;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class JdbcBackofficeActorReadAdapter implements BackofficeActorReadPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final OpaqueCursorCodec codec = new OpaqueCursorCodec();

    public JdbcBackofficeActorReadAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public QueryPage<BackofficeActorItem> listAgents(BackofficeActorQuery query) {
        return list(query, "agents", "code", "id");
    }

    @Override
    public QueryPage<BackofficeActorItem> listClients(BackofficeActorQuery query) {
        return list(query, "clients", "phone_number", "id");
    }

    @Override
    public QueryPage<BackofficeActorItem> listMerchants(BackofficeActorQuery query) {
        return list(query, "merchants", "code", "id");
    }

    private QueryPage<BackofficeActorItem> list(BackofficeActorQuery query, String table, String searchField, String idField) {
        int limit = QueryInputValidator.normalizeLimit(query.limit(), 20, 100);
        var cursor = codec.decode(query.cursor());
        boolean desc = QueryInputValidator.resolveSort(query.sort(), "createdAt");

        StringBuilder sql = new StringBuilder("SELECT " + idField + "::text AS actor_id, "+ searchField +" AS code, status, created_at FROM " + table + " WHERE 1=1");
        var params = new MapSqlParameterSource();
        if (query.query() != null && !query.query().isBlank()) { sql.append(" AND ").append(searchField).append(" ILIKE :q"); params.addValue("q", "%" + query.query().trim() + "%"); }
        if (query.status() != null && !query.status().isBlank()) { sql.append(" AND status = :status"); params.addValue("status", query.status()); }
        if (query.createdFrom() != null) { sql.append(" AND created_at >= :from"); params.addValue("from", query.createdFrom()); }
        if (query.createdTo() != null) { sql.append(" AND created_at <= :to"); params.addValue("to", query.createdTo()); }
        if (cursor != null) {
            sql.append(desc
                    ? " AND (created_at < :cursorCreatedAt OR (created_at = :cursorCreatedAt AND id < CAST(:cursorId AS uuid)))"
                    : " AND (created_at > :cursorCreatedAt OR (created_at = :cursorCreatedAt AND id > CAST(:cursorId AS uuid)))");
            params.addValue("cursorCreatedAt", cursor.createdAt());
            params.addValue("cursorId", cursor.id());
        }
        sql.append(" ORDER BY created_at ").append(desc ? "DESC" : "ASC").append(", id ").append(desc ? "DESC" : "ASC");
        sql.append(" LIMIT :limit");
        params.addValue("limit", limit + 1);

        List<BackofficeActorItem> rows = jdbcTemplate.query(sql.toString(), params, (rs, i) -> new BackofficeActorItem(
                rs.getString("actor_id"), rs.getString("code"), rs.getString("status"), rs.getTimestamp("created_at").toInstant()
        ));
        boolean hasMore = rows.size() > limit;
        if (hasMore) rows = new ArrayList<>(rows.subList(0, limit));
        String next = hasMore && !rows.isEmpty() ? codec.encode(rows.get(rows.size() - 1).createdAt(), UUID.fromString(rows.get(rows.size() - 1).actorId())) : null;
        return new QueryPage<>(rows, next, hasMore);
    }
}
