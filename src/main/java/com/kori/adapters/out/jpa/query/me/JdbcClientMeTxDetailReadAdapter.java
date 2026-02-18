package com.kori.adapters.out.jpa.query.me;

import com.kori.query.model.me.MeQueryModels;
import com.kori.query.port.out.ClientMeTxDetailReadPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JdbcClientMeTxDetailReadAdapter implements ClientMeTxDetailReadPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcClientMeTxDetailReadAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<MeQueryModels.ClientTransactionDetails> findOwnedByClient(String clientCode, String transactionRef) {
        String sql = """
                SELECT t.id::text AS transaction_ref,
                       t.type,
                       COALESCE(p.status, cr.status, 'COMPLETED') AS status,
                       t.amount,
                       owned.total_debited,
                       (owned.total_debited - t.amount) AS fee,
                       'KMF' AS currency,
                       m.code AS merchant_code,
                       t.original_transaction_id::text AS original_transaction_ref,
                       t.created_at
                FROM transactions t
                JOIN (
                      SELECT le.transaction_id,
                        COALESCE(SUM(CASE WHEN le.entry_type = 'DEBIT' THEN le.amount ELSE 0 END), 0) AS total_debited
                      FROM ledger_entries le
                      WHERE le.transaction_id::text = :transactionRef
                        AND le.account_type = 'CLIENT'
                        AND le.owner_ref = (SELECT c.id::text FROM clients c WHERE c.code = :clientCode)
                      GROUP BY le.transaction_id
                ) owned ON owned.transaction_id = t.id
                LEFT JOIN payouts p ON p.transaction_id = t.id
                LEFT JOIN client_refunds cr ON cr.transaction_id = t.id
                LEFT JOIN ledger_entries merchant_entry ON merchant_entry.transaction_id = t.id AND merchant_entry.account_type = 'MERCHANT'
                LEFT JOIN merchants m ON m.id::text = merchant_entry.owner_ref
                WHERE t.id::text = :transactionRef
                LIMIT 1
                """;
        var params = new MapSqlParameterSource()
                .addValue("clientCode", clientCode)
                .addValue("transactionRef", transactionRef);
        var rows = jdbcTemplate.query(sql, params, (rs, i) -> new MeQueryModels.ClientTransactionDetails(
                rs.getString("transaction_ref"),
                rs.getString("type"),
                rs.getString("status"),
                rs.getBigDecimal("amount"),
                rs.getBigDecimal("fee"),
                rs.getBigDecimal("total_debited"),
                rs.getString("currency"),
                rs.getString("merchant_code"),
                rs.getString("original_transaction_ref"),
                rs.getTimestamp("created_at").toInstant()
        ));
        return rows.stream().findFirst();
    }

    @Override
    public boolean existsTransaction(String transactionRef) {
        String sql = "SELECT COUNT(1) FROM transactions WHERE id::text = :transactionRef";
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("transactionRef", transactionRef), Integer.class);
        return count != null && count > 0;
    }
}
