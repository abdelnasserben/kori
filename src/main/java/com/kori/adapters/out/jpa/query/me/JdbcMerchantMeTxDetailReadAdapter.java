package com.kori.adapters.out.jpa.query.me;

import com.kori.query.model.me.MeQueryModels;
import com.kori.query.port.out.MerchantMeTxDetailReadPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JdbcMerchantMeTxDetailReadAdapter implements MerchantMeTxDetailReadPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcMerchantMeTxDetailReadAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<MeQueryModels.MerchantTransactionDetails> findOwnedByMerchant(String merchantCode, String transactionRef) {
        String sql = """
                SELECT t.id::text AS transaction_ref,
                       t.type,
                       COALESCE(p.status, cr.status, 'COMPLETED') AS status,
                       t.amount,
                       owned.total_debited,
                       (owned.total_debited - t.amount) AS fee,
                       'KMF' AS currency,
                       CASE WHEN t.type = 'MERCHANT_WITHDRAW_AT_AGENT' THEN a.code ELSE NULL END AS agent_code,
                           c.code AS client_code,
                           t.original_transaction_id::text AS original_transaction_ref
                       t.created_at
                FROM transactions t
                JOIN (
                     SELECT le.transaction_id,
                        COALESCE(SUM(CASE WHEN le.entry_type = 'DEBIT' THEN le.amount ELSE 0 END), 0) AS total_debited
                     FROM ledger_entries le
                     WHERE le.transaction_id::text = :transactionRef
                        AND le.account_type = 'MERCHANT'
                        AND le.owner_ref = (SELECT m.id::text FROM merchants m WHERE m.code = :merchantCode)
                     GROUP BY le.transaction_id
                ) owned ON owned.transaction_id = t.id
                LEFT JOIN payouts p ON p.transaction_id = t.id
                LEFT JOIN client_refunds cr ON cr.transaction_id = t.id
                LEFT JOIN ledger_entries agent_entry ON agent_entry.transaction_id = t.id
                AND agent_entry.account_type = 'AGENT_WALLET'
                LEFT JOIN agents a ON a.id::text = agent_entry.owner_ref
                LEFT JOIN ledger_entries client_entry ON client_entry.transaction_id = t.id 
                AND client_entry.account_type = 'CLIENT'
                LEFT JOIN clients c ON c.id::text = client_entry.owner_ref
                WHERE t.id::text = :transactionRef
                LIMIT 1
                """;
        var params = new MapSqlParameterSource()
                .addValue("merchantCode", merchantCode)
                .addValue("transactionRef", transactionRef);
        var rows = jdbcTemplate.query(sql, params, (rs, i) -> new MeQueryModels.MerchantTransactionDetails(
                rs.getString("transaction_ref"),
                rs.getString("type"),
                rs.getString("status"),
                rs.getBigDecimal("amount"),
                rs.getBigDecimal("fee"),
                rs.getBigDecimal("total_debited"),
                rs.getString("currency"),
                rs.getString("agent_code"),
                rs.getString("client_code"),
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
