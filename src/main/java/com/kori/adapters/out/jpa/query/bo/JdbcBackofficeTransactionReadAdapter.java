package com.kori.adapters.out.jpa.query.bo;

import com.kori.adapters.out.jpa.query.common.CursorPayload;
import com.kori.adapters.out.jpa.query.common.OpaqueCursorCodec;
import com.kori.adapters.out.jpa.query.common.QueryInputValidator;
import com.kori.application.exception.ValidationException;
import com.kori.application.port.out.query.BackofficeTransactionReadPort;
import com.kori.application.query.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.time.Instant;
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
        Sort sort = resolveSort(query.sort());

        StringBuilder sql = new StringBuilder(baseTransactionSql());

        MapSqlParameterSource params = new MapSqlParameterSource();
        applyFilters(query, sql, params);
        applyCursor(sql, params, cursor, sort);
        applySortAndLimit(sql, params, sort, limit + 1);

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
        String detailSql = baseTransactionSql() + " AND t.id = CAST(:id AS uuid) LIMIT 1";
        var params = new MapSqlParameterSource("id", transactionId);

        List<BackofficeTransactionDetails> details = jdbcTemplate.query(detailSql, params, (rs, rowNum) -> {
            String txId = rs.getString("id");
            var payout = readPayout(txId);
            var refund = readClientRefund(txId);
            var txMetadata = readTransactionMetadata(txId);
            var ledgerLines = readLedgerLines(txId);

            return new BackofficeTransactionDetails(
                    txId,
                    rs.getString("type"),
                    rs.getString("status"),
                    rs.getBigDecimal("amount"),
                    rs.getString("currency"),
                    rs.getString("merchant_code"),
                    rs.getString("agent_code"),
                    rs.getString("client_id"),
                    rs.getString("client_phone"),
                    rs.getString("merchant_id"),
                    rs.getString("agent_id"),
                    txMetadata.terminalUid(),
                    txMetadata.cardUid(),
                    rs.getString("original_transaction_id"),
                    payout,
                    refund,
                    ledgerLines,
                    rs.getTimestamp("created_at").toInstant()
            );
        });

        return details.stream().findFirst();
    }

    private String baseTransactionSql() {
        return """
                SELECT t.id,
                       t.type,
                       t.amount,
                       t.created_at,
                       t.original_transaction_id,
                       COALESCE(p.status, cr.status, 'COMPLETED') AS status,
                       'KMF' AS currency,
                       m.code AS merchant_code,
                       m.id::text AS merchant_id,
                       a.code AS agent_code,
                       a.id::text AS agent_id,
                       c.id::text AS client_id,
                       c.phone_number AS client_phone
                FROM transactions t
                LEFT JOIN payouts p ON p.transaction_id = t.id
                LEFT JOIN client_refunds cr ON cr.transaction_id = t.id
                LEFT JOIN LATERAL (
                       SELECT le.owner_ref
                       FROM ledger_entries le
                       WHERE le.transaction_id = t.id
                       AND le.account_type = 'MERCHANT'
                       ORDER BY le.created_at DESC NULLS LAST, le.id DESC
                       LIMIT 1
                ) lem ON TRUE
                LEFT JOIN merchants m ON m.id::text = lem.owner_ref
                LEFT JOIN LATERAL (
                       SELECT le.owner_ref
                       FROM ledger_entries le
                       WHERE le.transaction_id = t.id
                       AND le.account_type = 'AGENT_WALLET'
                       ORDER BY le.created_at DESC NULLS LAST, le.id DESC
                       LIMIT 1
                ) lea ON TRUE
                LEFT JOIN agents a ON a.id::text = lea.owner_ref
                LEFT JOIN LATERAL (
                       SELECT le.owner_ref
                       FROM ledger_entries le
                       WHERE le.transaction_id = t.id
                       AND le.account_type = 'CLIENT'
                       ORDER BY le.created_at DESC NULLS LAST, le.id DESC
                       LIMIT 1
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
            p.addValue("from", q.from());
        }
        if (q.to() != null) {
            sql.append(" AND t.created_at <= :to");
            p.addValue("to", q.to());
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
            sql.append(" AND (CAST(t.id AS text) ILIKE :q OR m.code ILIKE :q OR a.code ILIKE :q OR c.id::text ILIKE :q)");
            p.addValue("q", "%" + q.query().trim() + "%");
        }
        if (q.terminalUid() != null && !q.terminalUid().isBlank()) {
            sql.append("""
                     AND EXISTS (
                         SELECT 1 FROM audit_events ae
                         WHERE ae.metadata_json::jsonb ->> 'transactionId' = t.id::text
                           AND ae.metadata_json::jsonb ->> 'terminalUid' = :terminalUid
                     )
                    """);
            p.addValue("terminalUid", q.terminalUid().trim());
        }
        if (q.cardUid() != null && !q.cardUid().isBlank()) {
            sql.append("""
                     AND EXISTS (
                         SELECT 1 FROM audit_events ae
                         WHERE ae.metadata_json::jsonb ->> 'transactionId' = t.id::text
                           AND ae.metadata_json::jsonb ->> 'cardUid' = :cardUid
                     )
                    """);
            p.addValue("cardUid", q.cardUid().trim());
        }
        if (q.merchantId() != null && !q.merchantId().isBlank()) {
            sql.append(" AND m.id::text = :merchantId");
            p.addValue("merchantId", q.merchantId().trim());
        }
        if (q.agentId() != null && !q.agentId().isBlank()) {
            sql.append(" AND a.id::text = :agentId");
            p.addValue("agentId", q.agentId().trim());
        }
        if (q.clientPhone() != null && !q.clientPhone().isBlank()) {
            sql.append(" AND c.phone_number = :clientPhone");
            p.addValue("clientPhone", q.clientPhone().trim());
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

    private BackofficePayoutInfo readPayout(String transactionId) {
        String sql = """
                SELECT id::text AS payout_id, status, amount, created_at, completed_at, failed_at, failure_reason
                FROM payouts
                WHERE transaction_id = CAST(:id AS uuid)
                LIMIT 1
                """;
        var rows = jdbcTemplate.query(sql, new MapSqlParameterSource("id", transactionId), (rs, rowNum) -> new BackofficePayoutInfo(
                rs.getString("payout_id"),
                rs.getString("status"),
                rs.getBigDecimal("amount"),
                rs.getTimestamp("created_at").toInstant(),
                instantOrNull(rs.getTimestamp("completed_at")),
                instantOrNull(rs.getTimestamp("failed_at")),
                rs.getString("failure_reason")
        ));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private BackofficeClientRefundInfo readClientRefund(String transactionId) {
        String sql = """
                SELECT id::text AS refund_id, status, amount, created_at, completed_at, failed_at, failure_reason
                FROM client_refunds
                WHERE transaction_id = CAST(:id AS uuid)
                LIMIT 1
                """;
        var rows = jdbcTemplate.query(sql, new MapSqlParameterSource("id", transactionId), (rs, rowNum) -> new BackofficeClientRefundInfo(
                rs.getString("refund_id"),
                rs.getString("status"),
                rs.getBigDecimal("amount"),
                rs.getTimestamp("created_at").toInstant(),
                instantOrNull(rs.getTimestamp("completed_at")),
                instantOrNull(rs.getTimestamp("failed_at")),
                rs.getString("failure_reason")
        ));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private TransactionMetadata readTransactionMetadata(String transactionId) {
        String sql = """
                SELECT metadata_json::jsonb ->> 'terminalUid' AS terminal_uid,
                       metadata_json::jsonb ->> 'cardUid' AS card_uid,
                       occurred_at
                FROM audit_events
                WHERE metadata_json::jsonb ->> 'transactionId' = :transactionId
                ORDER BY occurred_at DESC, id DESC
                """;
        var rows = jdbcTemplate.query(sql, new MapSqlParameterSource("transactionId", transactionId), (rs, rowNum) -> new TransactionMetadata(
                rs.getString("terminal_uid"),
                rs.getString("card_uid"),
                rs.getTimestamp("occurred_at").toInstant()
        ));

        String terminalUid = null;
        String cardUid = null;
        for (TransactionMetadata row : rows) {
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

    private List<BackofficeLedgerLine> readLedgerLines(String transactionId) {
        String sql = """
                SELECT account_type, owner_ref, entry_type, amount
                FROM ledger_entries
                WHERE transaction_id = CAST(:id AS uuid)
                ORDER BY created_at ASC NULLS LAST, id ASC
                """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("id", transactionId), (rs, rowNum) -> new BackofficeLedgerLine(
                rs.getString("account_type"),
                rs.getString("owner_ref"),
                rs.getString("entry_type"),
                rs.getBigDecimal("amount"),
                "KMF"
        ));
    }

    private Instant instantOrNull(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record Sort(boolean desc) {}

    private record TransactionMetadata(String terminalUid, String cardUid, Instant occurredAt) {}
}
