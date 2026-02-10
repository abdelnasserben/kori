package com.kori.adapters.out.jpa.query.me;

import com.kori.application.exception.ValidationException;
import com.kori.application.port.out.query.MerchantMeReadPort;
import com.kori.application.query.QueryPage;
import com.kori.application.query.model.MeQueryModels;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class JdbcMerchantMeReadAdapter implements MerchantMeReadPort {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final Pattern SAFE_ENUM_FILTER = Pattern.compile("^[A-Z_]{2,64}$");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final MeOpaqueCursorCodec codec = new MeOpaqueCursorCodec();

    public JdbcMerchantMeReadAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<MeQueryModels.MeProfile> findProfile(String merchantId) {
        String sql = "SELECT id::text AS actor_id, code, status, created_at FROM merchants WHERE id = CAST(:id AS uuid) LIMIT 1";
        var rows = jdbcTemplate.query(sql, new MapSqlParameterSource("id", merchantId), (rs, n) ->
                new MeQueryModels.MeProfile(rs.getString("actor_id"), rs.getString("code"), rs.getString("status"), rs.getTimestamp("created_at").toInstant()));
        return rows.stream().findFirst();
    }

    @Override
    public MeQueryModels.MeBalance getBalance(String merchantId) {
        String sql = """
                SELECT COALESCE(SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE -amount END), 0) AS balance
                FROM ledger_entries
                WHERE account_type = 'MERCHANT' AND owner_ref = :ownerRef
                """;
        var balance = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("ownerRef", merchantId), java.math.BigDecimal.class);
        return new MeQueryModels.MeBalance("MERCHANT", merchantId, balance, "KMF");
    }

    @Override
    public QueryPage<MeQueryModels.MeTransactionItem> listTransactions(String merchantId, MeQueryModels.MeTransactionsFilter filter) {
        int limit = normalizeLimit(filter.limit());
        boolean desc = resolveCreatedAtSort(filter.sort());
        validateFilterToken("type", filter.type());
        validateFilterToken("status", filter.status());
        var cursor = codec.decode(filter.cursor());

        StringBuilder sql = new StringBuilder("""
                SELECT t.id, t.type, COALESCE(p.status, cr.status, 'COMPLETED') AS status, t.amount, t.created_at
                FROM transactions t
                JOIN ledger_entries le ON le.transaction_id = t.id
                LEFT JOIN payouts p ON p.transaction_id = t.id
                LEFT JOIN client_refunds cr ON cr.transaction_id = t.id
                WHERE le.account_type = 'MERCHANT' AND le.owner_ref = :ownerRef
                """);
        var params = new MapSqlParameterSource("ownerRef", merchantId);
        applyTransactionFilters(sql, params, filter);
        applyCursor(sql, params, cursor, desc);
        sql.append(" ORDER BY t.created_at ").append(desc ? "DESC" : "ASC").append(", t.id ").append(desc ? "DESC" : "ASC");
        sql.append(" LIMIT :limit");
        params.addValue("limit", limit + 1);

        List<MeQueryModels.MeTransactionItem> rows = jdbcTemplate.query(sql.toString(), params, (rs, n) -> new MeQueryModels.MeTransactionItem(
                rs.getString("id"), rs.getString("type"), rs.getString("status"), rs.getBigDecimal("amount"), "KMF", rs.getTimestamp("created_at").toInstant()
        ));
        boolean hasMore = rows.size() > limit;
        if (hasMore) rows = new ArrayList<>(rows.subList(0, limit));
        String next = hasMore && !rows.isEmpty() ? codec.encode(rows.get(rows.size() - 1).createdAt(), UUID.fromString(rows.get(rows.size() - 1).transactionId())) : null;
        return new QueryPage<>(rows, next, hasMore);
    }

    @Override
    public QueryPage<MeQueryModels.MeTerminalItem> listTerminals(String merchantId, MeQueryModels.MeTerminalsFilter filter) {
        int limit = normalizeLimit(filter.limit());
        boolean desc = resolveTerminalsSort(filter.sort());
        validateFilterToken("status", filter.status());
        var cursor = codec.decode(filter.cursor());

        StringBuilder sql = new StringBuilder("""
                SELECT t.id::text AS terminal_uid, t.status, t.created_at, t.merchant_id::text AS merchant_id,
                       (SELECT MAX(ae.occurred_at) FROM audit_events ae WHERE ae.actor_type = 'TERMINAL' AND ae.actor_id = t.id::text) AS last_seen
                FROM terminals t
                WHERE t.merchant_id = CAST(:merchantId AS uuid)
                """);
        var params = new MapSqlParameterSource("merchantId", merchantId);
        if (filter.status() != null && !filter.status().isBlank()) { sql.append(" AND t.status = :status"); params.addValue("status", filter.status()); }
        if (filter.query() != null && !filter.query().isBlank()) { sql.append(" AND t.id::text ILIKE :query"); params.addValue("query", "%" + filter.query().trim() + "%"); }
        if (cursor != null) {
            sql.append(desc
                    ? " AND (t.created_at < :cursorCreatedAt OR (t.created_at = :cursorCreatedAt AND t.id < CAST(:cursorId AS uuid)))"
                    : " AND (t.created_at > :cursorCreatedAt OR (t.created_at = :cursorCreatedAt AND t.id > CAST(:cursorId AS uuid)))");
            params.addValue("cursorCreatedAt", cursor.createdAt());
            params.addValue("cursorId", cursor.id());
        }
        sql.append(" ORDER BY t.created_at ").append(desc ? "DESC" : "ASC").append(", t.id ").append(desc ? "DESC" : "ASC");
        sql.append(" LIMIT :limit");
        params.addValue("limit", limit + 1);

        List<MeQueryModels.MeTerminalItem> rows = jdbcTemplate.query(sql.toString(), params, (rs, n) -> new MeQueryModels.MeTerminalItem(
                rs.getString("terminal_uid"), rs.getString("status"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("last_seen") == null ? null : rs.getTimestamp("last_seen").toInstant(), rs.getString("merchant_id")
        ));
        boolean hasMore = rows.size() > limit;
        if (hasMore) rows = new ArrayList<>(rows.subList(0, limit));
        String next = hasMore && !rows.isEmpty() ? codec.encode(rows.get(rows.size() - 1).createdAt(), UUID.fromString(rows.get(rows.size() - 1).terminalUid())) : null;
        return new QueryPage<>(rows, next, hasMore);
    }

    @Override
    public Optional<MeQueryModels.MeTerminalItem> findTerminalForMerchant(String merchantId, String terminalUid) {
        String sql = """
                SELECT t.id::text AS terminal_uid, t.status, t.created_at, t.merchant_id::text AS merchant_id,
                       (SELECT MAX(ae.occurred_at) FROM audit_events ae WHERE ae.actor_type = 'TERMINAL' AND ae.actor_id = t.id::text) AS last_seen
                FROM terminals t
                WHERE t.id = CAST(:terminalId AS uuid) AND t.merchant_id = CAST(:merchantId AS uuid)
                LIMIT 1
                """;
        var params = new MapSqlParameterSource().addValue("terminalId", terminalUid).addValue("merchantId", merchantId);
        var rows = jdbcTemplate.query(sql, params, (rs, n) -> new MeQueryModels.MeTerminalItem(
                rs.getString("terminal_uid"), rs.getString("status"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("last_seen") == null ? null : rs.getTimestamp("last_seen").toInstant(), rs.getString("merchant_id")
        ));
        return rows.stream().findFirst();
    }

    @Override
    public boolean existsTerminal(String terminalUid) {
        String sql = "SELECT COUNT(1) FROM terminals WHERE id = CAST(:terminalId AS uuid)";
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("terminalId", terminalUid), Integer.class);
        return count != null && count > 0;
    }

    private void applyTransactionFilters(StringBuilder sql, MapSqlParameterSource params, MeQueryModels.MeTransactionsFilter filter) {
        if (filter.type() != null && !filter.type().isBlank()) { sql.append(" AND t.type = :type"); params.addValue("type", filter.type()); }
        if (filter.status() != null && !filter.status().isBlank()) { sql.append(" AND COALESCE(p.status, cr.status, 'COMPLETED') = :status"); params.addValue("status", filter.status()); }
        if (filter.from() != null) { sql.append(" AND t.created_at >= :from"); params.addValue("from", filter.from()); }
        if (filter.to() != null) { sql.append(" AND t.created_at <= :to"); params.addValue("to", filter.to()); }
        if (filter.min() != null) { sql.append(" AND t.amount >= :min"); params.addValue("min", filter.min()); }
        if (filter.max() != null) { sql.append(" AND t.amount <= :max"); params.addValue("max", filter.max()); }
    }

    private void applyCursor(StringBuilder sql, MapSqlParameterSource params, MeOpaqueCursorCodec.CursorPayload cursor, boolean desc) {
        if (cursor == null) return;
        sql.append(desc
                ? " AND (t.created_at < :cursorCreatedAt OR (t.created_at = :cursorCreatedAt AND t.id < CAST(:cursorId AS uuid)))"
                : " AND (t.created_at > :cursorCreatedAt OR (t.created_at = :cursorCreatedAt AND t.id > CAST(:cursorId AS uuid)))");
        params.addValue("cursorCreatedAt", cursor.createdAt());
        params.addValue("cursorId", cursor.id());
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) return DEFAULT_LIMIT;
        if (limit < 1 || limit > MAX_LIMIT) throw new ValidationException("limit must be between 1 and 100", Map.of("field", "limit", "rejectedValue", limit));
        return limit;
    }

    private boolean resolveCreatedAtSort(String sortRaw) {
        if (sortRaw == null || sortRaw.isBlank()) return true;
        if (!"createdAt:desc".equals(sortRaw) && !"createdAt:asc".equals(sortRaw)) {
            throw new ValidationException("Invalid sort format. Use <field>:<asc|desc>", Map.of("field", "sort", "rejectedValue", sortRaw));
        }
        return sortRaw.endsWith("desc");
    }

    private boolean resolveTerminalsSort(String sortRaw) {
        return resolveCreatedAtSort(sortRaw);
    }

    private void validateFilterToken(String field, String value) {
        if (value == null || value.isBlank()) return;
        if (!SAFE_ENUM_FILTER.matcher(value).matches()) {
            throw new ValidationException("Invalid filter format", Map.of("field", field));
        }
    }
}
