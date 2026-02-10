package com.kori.adapters.out.jpa.query.bo;

import com.kori.adapters.out.jpa.query.common.CursorPayload;
import com.kori.adapters.out.jpa.query.common.OpaqueCursorCodec;
import com.kori.adapters.out.jpa.query.common.QueryInputValidator;
import com.kori.application.exception.ValidationException;
import com.kori.application.port.out.query.BackofficeTransactionReadPort;
import com.kori.application.query.BackofficeTransactionDetails;
import com.kori.application.query.BackofficeTransactionItem;
import com.kori.application.query.BackofficeTransactionQuery;
import com.kori.application.query.QueryPage;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JdbcBackofficeTransactionReadAdapter implements BackofficeTransactionReadPort {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final OpaqueCursorCodec codec = new OpaqueCursorCodec();

    public JdbcBackofficeTransactionReadAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public QueryPage<BackofficeTransactionItem> list(BackofficeTransactionQuery query) {
        int limit = QueryInputValidator.normalizeLimit(query.limit(), DEFAULT_LIMIT, MAX_LIMIT);
        var cursor = codec.decode(query.cursor());

        StringBuilder sql = new StringBuilder("""
                SELECT t.id, t.type, t.amount, t.created_at, t.original_transaction_id,
                       COALESCE(p.status, cr.status, 'COMPLETED') AS status,
                       'KMF' AS currency,
                       m.code AS merchant_code,
                       a.code AS agent_code,
                       c.id::text AS client_id
                FROM transactions t
                LEFT JOIN payouts p ON p.transaction_id = t.id
                LEFT JOIN client_refunds cr ON cr.transaction_id = t.id
                LEFT JOIN ledger_entries lem ON lem.transaction_id = t.id AND lem.account_type = 'MERCHANT'
                LEFT JOIN merchants m ON m.id::text = lem.owner_ref
                LEFT JOIN ledger_entries lea ON lea.transaction_id = t.id AND lea.account_type = 'AGENT_WALLET'
                LEFT JOIN agents a ON a.id::text = lea.owner_ref
                LEFT JOIN ledger_entries lec ON lec.transaction_id = t.id AND lec.account_type = 'CLIENT'
                LEFT JOIN clients c ON c.id::text = lec.owner_ref
                WHERE 1=1
                """);

        MapSqlParameterSource params = new MapSqlParameterSource();
        applyFilters(query, sql, params);
        applyCursor(sql, params, cursor, resolveSort(query.sort()));
        applySortAndLimit(sql, params, resolveSort(query.sort()), limit + 1);

        List<BackofficeTransactionItem> rows = jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> mapItem(rs));
        boolean hasMore = rows.size() > limit;
        if (hasMore) rows = new ArrayList<>(rows.subList(0, limit));

        String next = hasMore && !rows.isEmpty()
                ? codec.encode(rows.get(rows.size() - 1).createdAt(), UUID.fromString(rows.get(rows.size() - 1).transactionId()))
                : null;

        return new QueryPage<>(rows, next, hasMore);
    }

    @Override
    public Optional<BackofficeTransactionDetails> findById(String transactionId) {
        String sql = """
                SELECT t.id, t.type, t.amount, t.created_at, t.original_transaction_id,
                       COALESCE(p.status, cr.status, 'COMPLETED') AS status,
                       'KMF' AS currency,
                       m.code AS merchant_code,
                       a.code AS agent_code,
                       c.id::text AS client_id
                FROM transactions t
                LEFT JOIN payouts p ON p.transaction_id = t.id
                LEFT JOIN client_refunds cr ON cr.transaction_id = t.id
                LEFT JOIN ledger_entries lem ON lem.transaction_id = t.id AND lem.account_type = 'MERCHANT'
                LEFT JOIN merchants m ON m.id::text = lem.owner_ref
                LEFT JOIN ledger_entries lea ON lea.transaction_id = t.id AND lea.account_type = 'AGENT_WALLET'
                LEFT JOIN agents a ON a.id::text = lea.owner_ref
                LEFT JOIN ledger_entries lec ON lec.transaction_id = t.id AND lec.account_type = 'CLIENT'
                LEFT JOIN clients c ON c.id::text = lec.owner_ref
                WHERE t.id = CAST(:id AS uuid)
                LIMIT 1
                """;
        var list = jdbcTemplate.query(sql, new MapSqlParameterSource("id", transactionId), (rs, rowNum) -> new BackofficeTransactionDetails(
                rs.getString("id"),
                rs.getString("type"),
                rs.getString("status"),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                rs.getString("merchant_code"),
                rs.getString("agent_code"),
                rs.getString("client_id"),
                rs.getString("original_transaction_id"),
                rs.getTimestamp("created_at").toInstant()
        ));
        return list.stream().findFirst();
    }

    private void applyFilters(BackofficeTransactionQuery q, StringBuilder sql, MapSqlParameterSource p) {
        if (q.type() != null && !q.type().isBlank()) { sql.append(" AND t.type = :type"); p.addValue("type", q.type()); }
        if (q.status() != null && !q.status().isBlank()) { sql.append(" AND COALESCE(p.status, cr.status, 'COMPLETED') = :status"); p.addValue("status", q.status()); }
        if (q.from() != null) { sql.append(" AND t.created_at >= :from"); p.addValue("from", q.from()); }
        if (q.to() != null) { sql.append(" AND t.created_at <= :to"); p.addValue("to", q.to()); }
        if (q.min() != null) { sql.append(" AND t.amount >= :min"); p.addValue("min", q.min()); }
        if (q.max() != null) { sql.append(" AND t.amount <= :max"); p.addValue("max", q.max()); }
        if (q.query() != null && !q.query().isBlank()) {
            sql.append(" AND (CAST(t.id AS text) ILIKE :q OR m.code ILIKE :q OR a.code ILIKE :q OR c.id::text ILIKE :q)");
            p.addValue("q", "%" + q.query().trim() + "%");
        }
        if (q.actorType() != null && q.actorId() != null && !q.actorType().isBlank() && !q.actorId().isBlank()) {
            String normalized = q.actorType().trim().toUpperCase();
            switch (normalized) {
                case "MERCHANT" -> { sql.append(" AND m.id::text = :actorId"); p.addValue("actorId", q.actorId()); }
                case "AGENT" -> { sql.append(" AND a.id::text = :actorId"); p.addValue("actorId", q.actorId()); }
                case "CLIENT" -> { sql.append(" AND c.id::text = :actorId"); p.addValue("actorId", q.actorId()); }
                default -> throw new ValidationException("Unsupported actorType", java.util.Map.of("field", "actorType"));
            }
        }
    }

    private void applyCursor(StringBuilder sql, MapSqlParameterSource p, CursorPayload cursor, Sort sort) {
        if (cursor == null) return;
        sql.append(sort.desc ? " AND (t.created_at < :cursorCreatedAt OR (t.created_at = :cursorCreatedAt AND t.id < CAST(:cursorId AS uuid)))"
                : " AND (t.created_at > :cursorCreatedAt OR (t.created_at = :cursorCreatedAt AND t.id > CAST(:cursorId AS uuid)))");
        p.addValue("cursorCreatedAt", cursor.createdAt());
        p.addValue("cursorId", cursor.id());
    }

    private void applySortAndLimit(StringBuilder sql, MapSqlParameterSource p, Sort sort, int limit) {
        sql.append(" ORDER BY t.created_at ").append(sort.desc ? "DESC" : "ASC").append(", t.id ").append(sort.desc ? "DESC" : "ASC");
        sql.append(" LIMIT :limit");
        p.addValue("limit", limit);
    }

    private Sort resolveSort(String sortRaw) {
        return new Sort(QueryInputValidator.resolveSort(sortRaw, "createdAt"));
    }

    private BackofficeTransactionItem mapItem(ResultSet rs) throws java.sql.SQLException {
        return new BackofficeTransactionItem(
                rs.getString("id"),
                rs.getString("type"),
                rs.getString("status"),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                rs.getString("merchant_code"),
                rs.getString("agent_code"),
                rs.getString("client_id"),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private record Sort(boolean desc) {}
}
