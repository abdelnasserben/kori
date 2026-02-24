package com.kori.adapters.out.jpa.query.bo;

import com.kori.adapters.out.jpa.query.common.OpaqueCursorCodec;
import com.kori.adapters.out.jpa.query.common.QueryInputValidator;
import com.kori.application.exception.ValidationException;
import com.kori.query.model.*;
import com.kori.query.port.out.BackofficeTransactionReadPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        boolean desc = QueryInputValidator.resolveSort(query.sort(), "createdAt");

        StringBuilder sql = new StringBuilder(baseSql());

        MapSqlParameterSource params = new MapSqlParameterSource();
        applyFilters(query, sql, params);
        if (cursor != null) {
            sql.append(desc
                    ? " AND (t.created_at < :cursorCreatedAt OR (t.created_at = :cursorCreatedAt AND t.id::text < :cursorRef))"
                    : " AND (t.created_at > :cursorCreatedAt OR (t.created_at = :cursorCreatedAt AND t.id::text > :cursorRef))");
            params.addValue("cursorCreatedAt", Timestamp.from(cursor.createdAt()));
            params.addValue("cursorRef", cursor.ref());
        }
        sql.append(" ORDER BY t.created_at ").append(desc ? "DESC" : "ASC").append(", t.id ").append(desc ? "DESC" : "ASC");
        sql.append(" LIMIT :limit");
        params.addValue("limit", limit + 1);

        List<BackofficeTransactionItem> rows = jdbcTemplate.query(sql.toString(), params, (rs, n) -> new BackofficeTransactionItem(
                rs.getString("transaction_ref"),
                rs.getString("type"),
                rs.getString("status"),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                rs.getString("merchant_code"),
                rs.getString("agent_code"),
                rs.getString("client_code"),
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
    public Optional<BackofficeTransactionDetails> findByRef(String transactionRef) {
        String sql = baseSql() + " AND t.id::text = :transactionRef LIMIT 1";
        var rows = jdbcTemplate.query(sql, new MapSqlParameterSource("transactionRef", transactionRef), (rs, n) -> {
            var metadata = readTransactionMetadata(transactionRef);

            return new BackofficeTransactionDetails(
                    rs.getString("transaction_ref"),
                    rs.getString("type"),
                    rs.getString("status"),
                    rs.getBigDecimal("amount"),
                    rs.getString("currency"),
                    rs.getString("merchant_code"),
                    rs.getString("agent_code"),
                    rs.getString("client_code"),
                    rs.getString("client_phone"),
                    metadata.terminalUid(),
                    metadata.cardUid(),
                    rs.getString("original_transaction_ref"),
                    readPayout(transactionRef),
                    readClientRefund(transactionRef),
                    readLedgerLines(transactionRef),
                    rs.getTimestamp("created_at").toInstant());
        });

        return rows.stream().findFirst();
    }

    private String baseSql() {
        return """
                SELECT t.id::text AS transaction_ref,
                       t.type, t.amount,
                       t.created_at,
                       t.original_transaction_id::text AS original_transaction_ref,
                       COALESCE(p.status, cr.status, 'COMPLETED') AS status,
                       'KMF' AS currency,
                       m.code AS merchant_code,
                       a.code AS agent_code,
                       c.code AS client_code,
                       c.phone_number AS client_phone
                FROM transactions t
                LEFT JOIN payouts p ON p.transaction_id = t.id
                LEFT JOIN client_refunds cr ON cr.transaction_id = t.id
                LEFT JOIN LATERAL (
                       SELECT le.owner_ref FROM ledger_entries le
                       WHERE le.transaction_id = t.id AND le.account_type = 'MERCHANT'
                       ORDER BY le.created_at DESC NULLS LAST, le.id DESC LIMIT 1
                ) lem ON TRUE
                LEFT JOIN merchants m ON m.id::text = lem.owner_ref
                LEFT JOIN LATERAL (
                       SELECT le.owner_ref FROM ledger_entries le
                       WHERE le.transaction_id = t.id AND le.account_type = 'AGENT_WALLET'
                       ORDER BY le.created_at DESC NULLS LAST, le.id DESC LIMIT 1
                ) lea ON TRUE
                LEFT JOIN agents a ON a.id::text = lea.owner_ref
                LEFT JOIN LATERAL (
                       SELECT le.owner_ref FROM ledger_entries le
                       WHERE le.transaction_id = t.id AND le.account_type = 'CLIENT'
                       ORDER BY le.created_at DESC NULLS LAST, le.id DESC LIMIT 1
                ) lec ON TRUE
                LEFT JOIN clients c ON c.id::text = lec.owner_ref
                WHERE 1=1
                """;
    }

    private void applyFilters(BackofficeTransactionQuery q, StringBuilder sql, MapSqlParameterSource p) {
        if (q.type() != null && !q.type().isBlank()) {
            sql.append(" AND t.type = :type");
            p.addValue("type", q.type());
        }
        if (q.status() != null && !q.status().isBlank()) {
            sql.append(" AND COALESCE(p.status, cr.status, 'COMPLETED') = :status");
            p.addValue("status", q.status());
        }
        if (q.from() != null) {
            sql.append(" AND t.created_at >= :from");
            p.addValue("from", Timestamp.from(q.from()));
        }
        if (q.to() != null) {
            sql.append(" AND t.created_at <= :to");
            p.addValue("to", Timestamp.from(q.to()));
        }
        if (q.min() != null) {
            sql.append(" AND t.amount >= :min");
            p.addValue("min", q.min());
        }
        if (q.max() != null) {
            sql.append(" AND t.amount <= :max");
            p.addValue("max", q.max());
        }
        if (q.query() != null && !q.query().isBlank()) {
            sql.append(" AND (t.id::text ILIKE :q OR m.code ILIKE :q OR a.code ILIKE :q OR c.code ILIKE :q)");
            p.addValue("q", "%" + q.query().trim() + "%");
        }
        if (q.terminalUid() != null && !q.terminalUid().isBlank()) {
            sql.append(" AND EXISTS (SELECT 1 FROM audit_events ae WHERE ae.metadata_json::jsonb ->> 'transactionRef' = t.id::text AND ae.metadata_json::jsonb ->> 'terminalUid' = :terminalUid)");
            p.addValue("terminalUid", q.terminalUid().trim());
        }
        if (q.cardUid() != null && !q.cardUid().isBlank()) {
            sql.append(" AND EXISTS (SELECT 1 FROM audit_events ae WHERE ae.metadata_json::jsonb ->> 'transactionRef' = t.id::text AND ae.metadata_json::jsonb ->> 'cardUid' = :cardUid)");
            p.addValue("cardUid", q.cardUid().trim());
        }
        if (q.merchantCode() != null && !q.merchantCode().isBlank()) {
            sql.append(" AND m.code = :merchantCode");
            p.addValue("merchantCode", q.merchantCode().trim());
        }
        if (q.agentCode() != null && !q.agentCode().isBlank()) {
            sql.append(" AND a.code = :agentCode");
            p.addValue("agentCode", q.agentCode().trim());
        }
        if (q.clientPhone() != null && !q.clientPhone().isBlank()) {
            sql.append(" AND c.phone_number = :clientPhone");
            p.addValue("clientPhone", q.clientPhone().trim());
        }
        if (q.actorType() != null && q.actorRef() != null && !q.actorType().isBlank() && !q.actorRef().isBlank()) {
            switch (q.actorType().trim().toUpperCase()) {
                case "MERCHANT" -> {
                    sql.append(" AND m.code = :actorRef");
                    p.addValue("actorRef", q.actorRef());
                }
                case "AGENT" -> {
                    sql.append(" AND a.code = :actorRef");
                    p.addValue("actorRef", q.actorRef());
                }
                case "CLIENT" -> {
                    sql.append(" AND c.code = :actorRef");
                    p.addValue("actorRef", q.actorRef());
                }
                default -> throw new ValidationException("Unsupported actorType", Map.of("field", "actorType"));
            }
        }
    }

    private BackofficePayoutInfo readPayout(String transactionRef) {
        String sql = "SELECT id::text AS payout_ref, status, amount, created_at, completed_at, failed_at, failure_reason FROM payouts WHERE transaction_id::text = :transactionRef LIMIT 1";
        var rows = jdbcTemplate.query(sql, new MapSqlParameterSource("transactionRef", transactionRef), (rs, n) -> new BackofficePayoutInfo(
                rs.getString("payout_ref"),
                rs.getString("status"),
                rs.getBigDecimal("amount"),
                rs.getTimestamp("created_at").toInstant(),
                instantOrNull(rs.getTimestamp("completed_at")),
                instantOrNull(rs.getTimestamp("failed_at")),
                rs.getString("failure_reason")
        ));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private BackofficeClientRefundInfo readClientRefund(String transactionRef) {
        String sql = "SELECT id::text AS refund_ref, status, amount, created_at, completed_at, failed_at, failure_reason FROM client_refunds WHERE transaction_id::text = :transactionRef LIMIT 1";
        var rows = jdbcTemplate.query(sql, new MapSqlParameterSource("transactionRef", transactionRef), (rs, n) -> new BackofficeClientRefundInfo(
                rs.getString("refund_ref"),
                rs.getString("status"),
                rs.getBigDecimal("amount"),
                rs.getTimestamp("created_at").toInstant(),
                instantOrNull(rs.getTimestamp("completed_at")),
                instantOrNull(rs.getTimestamp("failed_at")),
                rs.getString("failure_reason")
        ));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private TransactionMetadata readTransactionMetadata(String transactionRef) {
        String sql = "SELECT metadata_json::jsonb ->> 'terminalUid' AS terminal_uid, metadata_json::jsonb ->> 'cardUid' AS card_uid, occurred_at FROM audit_events WHERE metadata_json::jsonb ->> 'transactionRef' = :transactionRef ORDER BY occurred_at DESC, id DESC";
        var rows = jdbcTemplate.query(sql, new MapSqlParameterSource("transactionRef", transactionRef), (rs, n) -> new TransactionMetadata(rs.getString("terminal_uid"), rs.getString("card_uid"), rs.getTimestamp("occurred_at").toInstant()));
        String terminalUid = null; String cardUid = null;
        for (var row : rows) {
            if (terminalUid == null && row.terminalUid() != null && !row.terminalUid().isBlank()) {
                terminalUid = row.terminalUid();
            }
            if (cardUid == null && row.cardUid() != null && !row.cardUid().isBlank()) {
                cardUid = row.cardUid();
            }
            if (terminalUid != null && cardUid != null) {
                break;
            }
        }
        return new TransactionMetadata(terminalUid, cardUid, null);
    }

    private List<BackofficeLedgerLine> readLedgerLines(String transactionRef) {
        String sql = """
                SELECT le.account_type,
                       CASE
                           WHEN le.account_type IN ('MERCHANT', 'MERCHANT_SETTLEMENT') THEN m.code
                           WHEN le.account_type IN ('AGENT_WALLET', 'AGENT_CASH_CLEARING') THEN a.code
                           WHEN le.account_type = 'CLIENT' THEN c.code
                           ELSE le.owner_ref
                       END AS owner_ref,
                       le.entry_type,
                       le.amount
                FROM ledger_entries le
                LEFT JOIN merchants m ON m.id::text = le.owner_ref
                LEFT JOIN agents a ON a.id::text = le.owner_ref
                LEFT JOIN clients c ON c.id::text = le.owner_ref
                WHERE le.transaction_id::text = :transactionRef
                ORDER BY le.created_at ASC NULLS LAST, le.id ASC
                """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("transactionRef", transactionRef), (rs, n) -> new BackofficeLedgerLine(
                rs.getString("account_type"),
                rs.getString("owner_ref"),
                rs.getString("entry_type"),
                rs.getBigDecimal("amount"),
                "KMF"
        ));
    }

    private Instant instantOrNull(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record TransactionMetadata(String terminalUid, String cardUid, Instant occurredAt) {}
}
