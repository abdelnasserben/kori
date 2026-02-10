package com.kori.adapters.out.jpa.query.me;

import com.kori.application.port.out.query.ClientMeTxDetailReadPort;
import com.kori.application.query.model.MeQueryModels;
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
    public Optional<MeQueryModels.ClientTransactionDetails> findOwnedByClient(String clientId, String transactionId) {
        String sql = """
                SELECT t.id::text AS transaction_id,
                       t.type,
                       COALESCE(p.status, cr.status, 'COMPLETED') AS status,
                       t.amount,
                       'KMF' AS currency,
                       m.code AS merchant_code,
                       t.original_transaction_id::text AS original_transaction_id,
                       t.created_at
                FROM transactions t
                JOIN ledger_entries lec ON lec.transaction_id = t.id
                        AND lec.account_type = 'CLIENT'
                        AND lec.owner_ref = :clientId
                LEFT JOIN payouts p ON p.transaction_id = t.id
                LEFT JOIN client_refunds cr ON cr.transaction_id = t.id
                LEFT JOIN ledger_entries lem ON lem.transaction_id = t.id AND lem.account_type = 'MERCHANT'
                LEFT JOIN merchants m ON m.id::text = lem.owner_ref
                WHERE t.id = CAST(:transactionId AS uuid)
                LIMIT 1
                """;
        var params = new MapSqlParameterSource()
                .addValue("clientId", clientId)
                .addValue("transactionId", transactionId);
        var rows = jdbcTemplate.query(sql, params, (rs, i) -> new MeQueryModels.ClientTransactionDetails(
                rs.getString("transaction_id"),
                rs.getString("type"),
                rs.getString("status"),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                rs.getString("merchant_code"),
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
