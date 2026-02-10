package com.kori.adapters.out.jpa.query.me;

import com.kori.application.port.out.query.MerchantMeTxDetailReadPort;
import com.kori.application.query.model.MeQueryModels;
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
    public Optional<MeQueryModels.MerchantTransactionDetails> findOwnedByMerchant(String merchantId, String transactionId) {
        String sql = """
                SELECT t.id::text AS transaction_id,
                       t.type,
                       COALESCE(p.status, cr.status, 'COMPLETED') AS status,
                       t.amount,
                       'KMF' AS currency,
                       a.code AS agent_code,
                       c.id::text AS client_id,
                       t.original_transaction_id::text AS original_transaction_id,
                       t.created_at
                FROM transactions t
                JOIN ledger_entries lem ON lem.transaction_id = t.id
                        AND lem.account_type = 'MERCHANT'
                        AND lem.owner_ref = :merchantId
                LEFT JOIN payouts p ON p.transaction_id = t.id
                LEFT JOIN client_refunds cr ON cr.transaction_id = t.id
                LEFT JOIN ledger_entries lea ON lea.transaction_id = t.id AND lea.account_type = 'AGENT_WALLET'
                LEFT JOIN agents a ON a.id::text = lea.owner_ref
                LEFT JOIN ledger_entries lec ON lec.transaction_id = t.id AND lec.account_type = 'CLIENT'
                LEFT JOIN clients c ON c.id::text = lec.owner_ref
                WHERE t.id = CAST(:transactionId AS uuid)
                LIMIT 1
                """;
        var params = new MapSqlParameterSource()
                .addValue("merchantId", merchantId)
                .addValue("transactionId", transactionId);
        var rows = jdbcTemplate.query(sql, params, (rs, i) -> new MeQueryModels.MerchantTransactionDetails(
                rs.getString("transaction_id"),
                rs.getString("type"),
                rs.getString("status"),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                rs.getString("agent_code"),
                rs.getString("client_id"),
                rs.getString("original_transaction_id"),
                rs.getTimestamp("created_at").toInstant()
        ));
        return rows.stream().findFirst();
    }

    @Override
    public boolean existsTransaction(String transactionId) {
        String sql = "SELECT COUNT(1) FROM transactions WHERE id = CAST(:transactionId AS uuid)";
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("transactionId", transactionId), Integer.class);
        return count != null && count > 0;
    }
}
