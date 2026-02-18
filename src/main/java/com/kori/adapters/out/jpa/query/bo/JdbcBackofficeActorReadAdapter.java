package com.kori.adapters.out.jpa.query.bo;

import com.kori.adapters.out.jpa.query.common.OpaqueCursorCodec;
import com.kori.adapters.out.jpa.query.common.QueryInputValidator;
import com.kori.query.model.BackofficeActorItem;
import com.kori.query.model.BackofficeActorQuery;
import com.kori.query.model.QueryPage;
import com.kori.query.port.out.BackofficeActorReadPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JdbcBackofficeActorReadAdapter implements BackofficeActorReadPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final OpaqueCursorCodec codec = new OpaqueCursorCodec();

    public JdbcBackofficeActorReadAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public QueryPage<BackofficeActorItem> listAgents(BackofficeActorQuery query) {
        return list(query, "agents", "code");
    }

    @Override
    public QueryPage<BackofficeActorItem> listClients(BackofficeActorQuery query) {
        return list(query, "clients", "code");
    }

    @Override
    public QueryPage<BackofficeActorItem> listMerchants(BackofficeActorQuery query) {
        return list(query, "merchants", "code");
    }

    private QueryPage<BackofficeActorItem> list(BackofficeActorQuery query, String table, String codeField) {
        int limit = QueryInputValidator.normalizeLimit(query.limit(), 20, 100);
        var cursor = codec.decode(query.cursor());
        boolean desc = QueryInputValidator.resolveSort(query.sort(), "createdAt");

        StringBuilder sql = new StringBuilder("SELECT " + codeField + " AS actor_ref, " + codeField + " AS code, status, created_at FROM " + table + " WHERE 1=1");
        var params = new MapSqlParameterSource();
        if (query.query() != null && !query.query().isBlank()) {
            sql.append(" AND ").append(codeField).append(" ILIKE :q");
            params.addValue("q", "%" + query.query().trim() + "%");
        }
        if (query.status() != null && !query.status().isBlank()) {
            sql.append(" AND status = :status");
            params.addValue("status", query.status());
        }
        if (query.createdFrom() != null) {
            sql.append(" AND created_at >= :from");
            params.addValue("from", query.createdFrom());
        }
        if (query.createdTo() != null) {
            sql.append(" AND created_at <= :to");
            params.addValue("to", query.createdTo());
        }
        if (cursor != null) {
            sql.append(desc
                    ? " AND (created_at < :cursorCreatedAt OR (created_at = :cursorCreatedAt AND " + codeField + " < :cursorRef))"
                    : " AND (created_at > :cursorCreatedAt OR (created_at = :cursorCreatedAt AND " + codeField + " > :cursorRef))");
            params.addValue("cursorCreatedAt", cursor.createdAt());
            params.addValue("cursorRef", cursor.ref());
        }
        sql.append(" ORDER BY created_at ").append(desc ? "DESC" : "ASC").append(", ").append(codeField).append(desc ? " DESC" : " ASC");
        sql.append(" LIMIT :limit");
        params.addValue("limit", limit + 1);

        List<BackofficeActorItem> rows = jdbcTemplate.query(sql.toString(), params, (rs, i) -> new BackofficeActorItem(
                rs.getString("actor_ref"),
                rs.getString("code"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant()
        ));
        boolean hasMore = rows.size() > limit;
        if (hasMore) rows = new ArrayList<>(rows.subList(0, limit));
        String next = hasMore && !rows.isEmpty()
                ? codec.encode(rows.get(rows.size() - 1).createdAt(), rows.get(rows.size() - 1).actorRef())
                : null;
        return new QueryPage<>(rows, next, hasMore);
    }
}
