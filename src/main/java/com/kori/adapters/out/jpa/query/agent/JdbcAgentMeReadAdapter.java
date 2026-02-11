package com.kori.adapters.out.jpa.query.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kori.adapters.out.jpa.query.common.CursorPayload;
import com.kori.adapters.out.jpa.query.common.OpaqueCursorCodec;
import com.kori.adapters.out.jpa.query.common.QueryInputValidator;
import com.kori.query.model.QueryPage;
import com.kori.query.model.me.AgentQueryModels;
import com.kori.query.port.out.AgentMeReadPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class JdbcAgentMeReadAdapter implements AgentMeReadPort {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final OpaqueCursorCodec codec = new OpaqueCursorCodec();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JdbcAgentMeReadAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<AgentQueryModels.AgentSummary> findSummary(String agentId) {
        String sql = """
                SELECT a.id::text AS agent_id,
                       a.code,
                       a.status,
                       COALESCE((
                           SELECT SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE -le.amount END)
                           FROM ledger_entries le
                           WHERE le.account_type = 'AGENT_CASH_CLEARING' AND le.owner_ref = a.id::text
                       ), 0) AS cash_balance,
                       COALESCE((
                           SELECT SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE -le.amount END)
                           FROM ledger_entries le
                           WHERE le.account_type = 'AGENT_WALLET' AND le.owner_ref = a.id::text
                       ), 0) AS commission_balance,
                       (
                           SELECT COUNT(DISTINCT t.id)
                           FROM transactions t
                           JOIN ledger_entries le ON le.transaction_id = t.id
                           WHERE le.account_type IN ('AGENT_CASH_CLEARING', 'AGENT_WALLET')
                             AND le.owner_ref = a.id::text
                             AND t.created_at >= NOW() - INTERVAL '7 days'
                       ) AS tx_count_7d
                FROM agents a
                WHERE a.id = CAST(:agentCode AS uuid)
                LIMIT 1
                """;
        var rows = jdbcTemplate.query(sql, new MapSqlParameterSource("agentCode", agentId), (rs, n) ->
                new AgentQueryModels.AgentSummary(
                        rs.getString("agent_id"),
                        rs.getString("code"),
                        rs.getString("status"),
                        rs.getBigDecimal("cash_balance"),
                        rs.getBigDecimal("commission_balance"),
                        rs.getLong("tx_count_7d")
                ));
        return rows.stream().findFirst();
    }

    @Override
    public QueryPage<AgentQueryModels.AgentTransactionItem> listTransactions(String agentId, AgentQueryModels.AgentTransactionFilter filter) {
        int limit = QueryInputValidator.normalizeLimit(filter.limit(), DEFAULT_LIMIT, MAX_LIMIT);
        boolean desc = QueryInputValidator.resolveSort(filter.sort(), "createdAt");
        QueryInputValidator.validateEnumFilter("type", filter.type());
        QueryInputValidator.validateEnumFilter("status", filter.status());
        CursorPayload cursor = codec.decode(filter.cursor());

        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT t.id::text AS id,
                       t.type,
                       COALESCE(p.status, cr.status, 'COMPLETED') AS status,
                       t.amount,
                       t.created_at
                FROM transactions t
                JOIN ledger_entries le ON le.transaction_id = t.id
                LEFT JOIN payouts p ON p.transaction_id = t.id
                LEFT JOIN client_refunds cr ON cr.transaction_id = t.id
                WHERE le.owner_ref = :ownerRef
                  AND le.account_type IN ('AGENT_CASH_CLEARING', 'AGENT_WALLET')
                """);
        var params = new MapSqlParameterSource("ownerRef", agentId);

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

        applyTransactionCursor(sql, params, cursor, desc);
        sql.append(" ORDER BY t.created_at ").append(desc ? "DESC" : "ASC").append(", t.id ").append(desc ? "DESC" : "ASC");
        sql.append(" LIMIT :limit");
        params.addValue("limit", limit + 1);

        List<AgentQueryModels.AgentTransactionItem> rows = jdbcTemplate.query(sql.toString(), params, (rs, n) -> new AgentQueryModels.AgentTransactionItem(
                rs.getString("id"),
                rs.getString("type"),
                rs.getString("status"),
                rs.getBigDecimal("amount"),
                "KMF",
                rs.getTimestamp("created_at").toInstant()
        ));

        boolean hasMore = rows.size() > limit;
        if (hasMore) {
            rows = new ArrayList<>(rows.subList(0, limit));
        }
        String nextCursor = hasMore && !rows.isEmpty()
                ? codec.encode(rows.get(rows.size() - 1).createdAt(), UUID.fromString(rows.get(rows.size() - 1).transactionId()))
                : null;

        return new QueryPage<>(rows, nextCursor, hasMore);
    }

    @Override
    public QueryPage<AgentQueryModels.AgentActivityItem> listActivities(String agentId, AgentQueryModels.AgentActivityFilter filter) {
        int limit = QueryInputValidator.normalizeLimit(filter.limit(), DEFAULT_LIMIT, MAX_LIMIT);
        boolean desc = QueryInputValidator.resolveSort(filter.sort(), "occurredAt");
        QueryInputValidator.validateEnumFilter("action", filter.action());
        CursorPayload cursor = codec.decode(filter.cursor());

        StringBuilder sql = new StringBuilder("""
                SELECT id::text AS event_id,
                       occurred_at,
                       action,
                       actor_type AS resource_type,
                       actor_id AS resource_id,
                       metadata_json
                FROM audit_events
                WHERE actor_type = 'AGENT'
                  AND actor_id = :actorRef
                """);
        var params = new MapSqlParameterSource("actorRef", agentId);

        if (filter.action() != null && !filter.action().isBlank()) {
            sql.append(" AND action = :action");
            params.addValue("action", filter.action());
        }
        if (filter.from() != null) {
            sql.append(" AND occurred_at >= :from");
            params.addValue("from", filter.from());
        }
        if (filter.to() != null) {
            sql.append(" AND occurred_at <= :to");
            params.addValue("to", filter.to());
        }

        applyActivityCursor(sql, params, cursor, desc);
        sql.append(" ORDER BY occurred_at ").append(desc ? "DESC" : "ASC").append(", id ").append(desc ? "DESC" : "ASC");
        sql.append(" LIMIT :limit");
        params.addValue("limit", limit + 1);

        List<AgentQueryModels.AgentActivityItem> rows = jdbcTemplate.query(sql.toString(), params, (rs, n) -> new AgentQueryModels.AgentActivityItem(
                rs.getString("event_id"),
                rs.getTimestamp("occurred_at").toInstant(),
                rs.getString("action"),
                rs.getString("resource_type"),
                rs.getString("resource_id"),
                parseMetadata(rs.getString("metadata_json"))
        ));

        boolean hasMore = rows.size() > limit;
        if (hasMore) {
            rows = new ArrayList<>(rows.subList(0, limit));
        }
        String nextCursor = hasMore && !rows.isEmpty()
                ? codec.encode(rows.get(rows.size() - 1).occurredAt(), UUID.fromString(rows.get(rows.size() - 1).eventId()))
                : null;

        return new QueryPage<>(rows, nextCursor, hasMore);
    }

    private void applyTransactionCursor(StringBuilder sql, MapSqlParameterSource params, CursorPayload cursor, boolean desc) {
        if (cursor == null) {
            return;
        }
        sql.append(desc
                ? " AND (t.created_at < :cursorCreatedAt OR (t.created_at = :cursorCreatedAt AND t.id < CAST(:cursorId AS uuid)))"
                : " AND (t.created_at > :cursorCreatedAt OR (t.created_at = :cursorCreatedAt AND t.id > CAST(:cursorId AS uuid)))");
        params.addValue("cursorCreatedAt", cursor.createdAt());
        params.addValue("cursorId", cursor.id());
    }

    private void applyActivityCursor(StringBuilder sql, MapSqlParameterSource params, CursorPayload cursor, boolean desc) {
        if (cursor == null) {
            return;
        }
        sql.append(desc
                ? " AND (occurred_at < :cursorCreatedAt OR (occurred_at = :cursorCreatedAt AND id < CAST(:cursorId AS uuid)))"
                : " AND (occurred_at > :cursorCreatedAt OR (occurred_at = :cursorCreatedAt AND id > CAST(:cursorId AS uuid)))");
        params.addValue("cursorCreatedAt", cursor.createdAt());
        params.addValue("cursorId", cursor.id());
    }

    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<>() {
            });
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }
}
