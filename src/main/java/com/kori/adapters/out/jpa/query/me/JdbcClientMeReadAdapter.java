package com.kori.adapters.out.jpa.query.me;

import com.kori.adapters.out.jpa.query.common.CursorPayload;
import com.kori.adapters.out.jpa.query.common.OpaqueCursorCodec;
import com.kori.adapters.out.jpa.query.common.QueryInputValidator;
import com.kori.query.model.QueryPage;
import com.kori.query.model.me.MeQueryModels;
import com.kori.query.port.out.ClientMeReadPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class JdbcClientMeReadAdapter implements ClientMeReadPort {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final OpaqueCursorCodec codec = new OpaqueCursorCodec();

    public JdbcClientMeReadAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<MeQueryModels.MeProfile> findProfile(String clientCode) {
        String sql = "SELECT c.code AS actor_ref, c.phone_number AS phone, c.status, c.created_at FROM clients c WHERE c.code = :clientCode LIMIT 1";
        var rows = jdbcTemplate.query(sql, new MapSqlParameterSource("clientCode", clientCode), (rs, n) ->
                new MeQueryModels.MeProfile(
                        rs.getString("actor_ref"),
                        rs.getString("phone"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant()));
        return rows.stream().findFirst();
    }

    @Override
    public MeQueryModels.MeBalance getBalance(String clientCode) {
        String sql = """
                SELECT COALESCE(SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE -amount END), 0) AS balance
                FROM ledger_entries
                WHERE account_type = 'CLIENT' AND owner_ref = (SELECT c.id::text FROM clients c WHERE c.code = :clientCode)
                """;
        var balance = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("clientCode", clientCode), java.math.BigDecimal.class);
        return new MeQueryModels.MeBalance("CLIENT", clientCode, balance, "KMF");
    }

    @Override
    public List<MeQueryModels.MeCardItem> listCards(String clientCode) {
        String sql = """
                SELECT card_uid, status, created_at
                FROM cards
                WHERE client_id = (SELECT c.id FROM clients c WHERE c.code = :clientCode)
                ORDER BY created_at DESC, id DESC
                """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("clientCode", clientCode), (rs, n) ->
                new MeQueryModels.MeCardItem(rs.getString("card_uid"), rs.getString("status"), rs.getTimestamp("created_at").toInstant()));
    }

    @Override
    public QueryPage<MeQueryModels.MeTransactionItem> listTransactions(String clientCode, MeQueryModels.MeTransactionsFilter filter) {
        int limit = QueryInputValidator.normalizeLimit(filter.limit(), DEFAULT_LIMIT, MAX_LIMIT);
        boolean desc = QueryInputValidator.resolveSort(filter.sort(), "createdAt");
        QueryInputValidator.validateEnumFilter("type", filter.type());
        QueryInputValidator.validateEnumFilter("status", filter.status());
        var cursor = codec.decode(filter.cursor());

        StringBuilder sql = new StringBuilder("""
                SELECT t.id::text AS transaction_ref,
                t.type,
                COALESCE(p.status, cr.status, 'COMPLETED') AS status,
                t.amount,
                t.created_at
                FROM transactions t
                LEFT JOIN payouts p ON p.transaction_id = t.id
                LEFT JOIN client_refunds cr ON cr.transaction_id = t.id
                WHERE EXISTS (
                     SELECT 1
                     FROM ledger_entries le
                     WHERE le.transaction_id = t.id
                     AND le.account_type = 'CLIENT'
                     AND le.owner_ref = (SELECT c.id::text FROM clients c WHERE c.code = :clientCode)
                )
                """);
        var params = new MapSqlParameterSource("clientCode", clientCode);
        applyTransactionFilters(sql, params, filter);
        applyCursor(sql, params, cursor, desc);
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

    private void applyTransactionFilters(StringBuilder sql, MapSqlParameterSource params, MeQueryModels.MeTransactionsFilter filter) {
        if (filter.type() != null && !filter.type().isBlank()) { sql.append(" AND t.type = :type"); params.addValue("type", filter.type()); }
        if (filter.status() != null && !filter.status().isBlank()) { sql.append(" AND COALESCE(p.status, cr.status, 'COMPLETED') = :status"); params.addValue("status", filter.status()); }
        if (filter.from() != null) { sql.append(" AND t.created_at >= :from"); params.addValue("from", filter.from()); }
        if (filter.to() != null) { sql.append(" AND t.created_at <= :to"); params.addValue("to", filter.to()); }
        if (filter.min() != null) { sql.append(" AND t.amount >= :min"); params.addValue("min", filter.min()); }
        if (filter.max() != null) { sql.append(" AND t.amount <= :max"); params.addValue("max", filter.max()); }
    }

    private void applyCursor(StringBuilder sql, MapSqlParameterSource params, CursorPayload cursor, boolean desc) {
        if (cursor == null) return;
        sql.append(desc
                ? " AND (t.created_at < :cursorCreatedAt OR (t.created_at = :cursorCreatedAt AND t.id::text < :cursorRef))"
                : " AND (t.created_at > :cursorCreatedAt OR (t.created_at = :cursorCreatedAt AND t.id::text > :cursorRef))");
        params.addValue("cursorCreatedAt", cursor.createdAt());
        params.addValue("cursorRef", cursor.ref());
    }
}
