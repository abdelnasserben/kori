package com.kori.adapters.out.jpa.query.me;

import com.kori.adapters.out.jpa.query.common.CursorPayload;
import com.kori.adapters.out.jpa.query.common.OpaqueCursorCodec;
import com.kori.adapters.out.jpa.query.common.QueryInputValidator;
import com.kori.query.model.QueryPage;
import com.kori.query.model.me.MeQueryModels;
import com.kori.query.port.out.MerchantMeReadPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class JdbcMerchantMeReadAdapter implements MerchantMeReadPort {
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final OpaqueCursorCodec codec = new OpaqueCursorCodec();

    public JdbcMerchantMeReadAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<MeQueryModels.MeProfile> findProfile(String merchantCode) {
        String sql = "SELECT code AS actor_ref, code, status, created_at FROM merchants WHERE code = :merchantCode LIMIT 1";
        var rows = jdbcTemplate.query(sql, new MapSqlParameterSource("merchantCode", merchantCode), (rs, n) ->
                new MeQueryModels.MeProfile(
                        rs.getString("actor_ref"),
                        rs.getString("code"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant()
                ));
        return rows.stream().findFirst();
    }

    @Override
    public MeQueryModels.MeBalance getBalance(String merchantCode) {
        String sql = """
                SELECT COALESCE(SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE -le.amount END), 0) AS balance
                FROM ledger_entries le
                WHERE le.account_type = 'MERCHANT'
                AND le.owner_ref = (SELECT m.id::text FROM merchants m WHERE m.code = :merchantCode)
                """;
        var balance = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("merchantCode", merchantCode), java.math.BigDecimal.class);
        return new MeQueryModels.MeBalance("MERCHANT", merchantCode, balance, "KMF");
    }

    @Override
    public QueryPage<MeQueryModels.MeTransactionItem> listTransactions(String merchantCode, MeQueryModels.MeTransactionsFilter filter) {
        int limit = QueryInputValidator.normalizeLimit(filter.limit(), DEFAULT_LIMIT, MAX_LIMIT);
        boolean desc = QueryInputValidator.resolveSort(filter.sort(), "createdAt");
        QueryInputValidator.validateEnumFilter("type", filter.type());
        QueryInputValidator.validateEnumFilter("status", filter.status());
        var cursor = codec.decode(filter.cursor());

        StringBuilder sql = new StringBuilder("""
                SELECT t.id::text AS transaction_ref, t.type, COALESCE(p.status, cr.status, 'COMPLETED') AS status, t.amount, t.created_at
                FROM transactions t
                LEFT JOIN payouts p ON p.transaction_id = t.id
                LEFT JOIN client_refunds cr ON cr.transaction_id = t.id
                WHERE EXISTS (
                     SELECT 1 FROM ledger_entries le
                     WHERE le.transaction_id = t.id
                     AND le.account_type = 'MERCHANT'
                     AND le.owner_ref = (SELECT m.id::text FROM merchants m WHERE m.code = :merchantCode)
                )
                """);
        var params = new MapSqlParameterSource("merchantCode", merchantCode);
        applyTransactionFilters(sql, params, filter);
        applyTransactionCursor(sql, params, cursor, desc);
        sql.append(" ORDER BY t.created_at ").append(desc ? "DESC" : "ASC").append(", t.id ").append(desc ? "DESC" : "ASC");
        sql.append(" LIMIT :limit");
        params.addValue("limit", limit + 1);

        List<MeQueryModels.MeTransactionItem> rows = jdbcTemplate.query(sql.toString(), params, (rs, n) -> new MeQueryModels.MeTransactionItem(
                rs.getString("transaction_ref"),
                rs.getString("type"),
                rs.getString("status"),
                rs.getBigDecimal("amount"),
                "KMF",
                rs.getTimestamp("created_at").toInstant()
        ));
        boolean hasMore = rows.size() > limit;
        if (hasMore) rows = new ArrayList<>(rows.subList(0, limit));
        String next = hasMore && !rows.isEmpty()
                ? codec.encode(rows.get(rows.size() - 1).createdAt(), rows.get(rows.size() - 1).transactionRef())
                : null;
        return new QueryPage<>(rows, next, hasMore);
    }

    @Override
    public QueryPage<MeQueryModels.MeTerminalItem> listTerminals(String merchantCode, MeQueryModels.MeTerminalsFilter filter) {
        int limit = QueryInputValidator.normalizeLimit(filter.limit(), DEFAULT_LIMIT, MAX_LIMIT);
        boolean desc = QueryInputValidator.resolveSort(filter.sort(), "createdAt");
        QueryInputValidator.validateEnumFilter("status", filter.status());
        var cursor = codec.decode(filter.cursor());

        StringBuilder sql = new StringBuilder("""
                SELECT t.terminal_uid, t.status, t.created_at, m.code AS merchant_code,
                   (SELECT MAX(ae.occurred_at) FROM audit_events ae WHERE ae.actor_type = 'TERMINAL' AND ae.actor_id = t.terminal_uid) AS last_seen
                FROM terminals t
                JOIN merchants m ON m.id = t.merchant_id
                WHERE m.code = :merchantCode
                """);
        var params = new MapSqlParameterSource("merchantCode", merchantCode);
        if (filter.status() != null && !filter.status().isBlank()) {
            sql.append(" AND t.status = :status");
            params.addValue("status", filter.status());
        }
        if (filter.terminalUid() != null && !filter.terminalUid().isBlank()) {
            sql.append(" AND t.terminal_uid ILIKE :terminalUid");
            params.addValue("terminalUid", "%" + filter.terminalUid().trim() + "%");
        }
        if (cursor != null) {
            sql.append(desc
                    ? " AND (t.created_at < :cursorCreatedAt OR (t.created_at = :cursorCreatedAt AND t.terminal_uid < :cursorRef))"
                    : " AND (t.created_at > :cursorCreatedAt OR (t.created_at = :cursorCreatedAt AND t.terminal_uid > :cursorRef))");
            params.addValue("cursorCreatedAt", cursor.createdAt());
            params.addValue("cursorRef", cursor.ref());
        }
        sql.append(" ORDER BY t.created_at ").append(desc ? "DESC" : "ASC").append(", t.terminal_uid ").append(desc ? "DESC" : "ASC");
        sql.append(" LIMIT :limit");
        params.addValue("limit", limit + 1);

        List<MeQueryModels.MeTerminalItem> rows = jdbcTemplate.query(sql.toString(), params, (rs, n) -> new MeQueryModels.MeTerminalItem(
                rs.getString("terminal_uid"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("last_seen") == null ? null : rs.getTimestamp("last_seen").toInstant(), rs.getString("merchant_code")
        ));
        boolean hasMore = rows.size() > limit;
        if (hasMore) rows = new ArrayList<>(rows.subList(0, limit));
        String next = hasMore && !rows.isEmpty()
                ? codec.encode(rows.get(rows.size() - 1).createdAt(), rows.get(rows.size() - 1).terminalUid())
                : null;
        return new QueryPage<>(rows, next, hasMore);
    }

    @Override
    public Optional<MeQueryModels.MeTerminalItem> findTerminalForMerchant(String merchantCode, String terminalUid) {
        String sql = """
                SELECT t.terminal_uid, t.status, t.created_at, m.code AS merchant_code,
                   (SELECT MAX(ae.occurred_at) FROM audit_events ae WHERE ae.actor_type = 'TERMINAL' AND ae.actor_id = t.terminal_uid) AS last_seen
                FROM terminals t
                JOIN merchants m ON m.id = t.merchant_id
                WHERE t.terminal_uid = :terminalUid AND m.code = :merchantCode
                LIMIT 1
                """;
        var params = new MapSqlParameterSource().addValue("terminalUid", terminalUid).addValue("merchantCode", merchantCode);
        var rows = jdbcTemplate.query(sql, params, (rs, n) -> new MeQueryModels.MeTerminalItem(
                rs.getString("terminal_uid"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("last_seen") == null ? null : rs.getTimestamp("last_seen").toInstant(), rs.getString("merchant_code")
        ));
        return rows.stream().findFirst();
    }

    @Override
    public boolean existsTerminal(String terminalUid) {
        String sql = "SELECT COUNT(1) FROM terminals WHERE terminal_uid = :terminalUid";
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("terminalUid", terminalUid), Integer.class);
        return count != null && count > 0;
    }

    private void applyTransactionFilters(StringBuilder sql, MapSqlParameterSource params, MeQueryModels.MeTransactionsFilter filter) {
        if (filter.type() != null && !filter.type().isBlank()) {
            sql.append(" AND t.type = :type");
            params.addValue("type", filter.type());
        }
        if (filter.status() != null && !filter.status().isBlank()) {
            sql.append(" AND COALESCE(p.status, cr.status, 'COMPLETED') = :status");
            params.addValue("status", filter.status());
        }
        if (filter.from() != null) {
            sql.append(" AND t.created_at >= :from");
            params.addValue("from", filter.from());
        }
        if (filter.to() != null) {
            sql.append(" AND t.created_at <= :to");
            params.addValue("to", filter.to());
        }
        if (filter.min() != null) {
            sql.append(" AND t.amount >= :min");
            params.addValue("min", filter.min());
        }
        if (filter.max() != null) {
            sql.append(" AND t.amount <= :max");
            params.addValue("max", filter.max());
        }
    }

    private void applyTransactionCursor(StringBuilder sql, MapSqlParameterSource params, CursorPayload cursor, boolean desc) {
        if (cursor == null) return;
        sql.append(desc
                ? " AND (t.created_at < :cursorCreatedAt OR (t.created_at = :cursorCreatedAt AND t.id::text < :cursorRef))"
                : " AND (t.created_at > :cursorCreatedAt OR (t.created_at = :cursorCreatedAt AND t.id::text > :cursorRef))");
        params.addValue("cursorCreatedAt", cursor.createdAt());
        params.addValue("cursorRef", cursor.ref());
    }
}
