package com.kori.adapters.out.jpa.query.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kori.adapters.out.jpa.query.common.CursorPayload;
import com.kori.adapters.out.jpa.query.common.OpaqueCursorCodec;
import com.kori.adapters.out.jpa.query.common.QueryInputValidator;
import com.kori.adapters.out.jpa.query.common.ReferenceResolver;
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
    private final ReferenceResolver referenceResolver;
    private final OpaqueCursorCodec codec = new OpaqueCursorCodec();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JdbcAgentMeReadAdapter(NamedParameterJdbcTemplate jdbcTemplate, ReferenceResolver referenceResolver) {
        this.jdbcTemplate = jdbcTemplate;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public Optional<AgentQueryModels.AgentSummary> findSummary(String agentCode) {
        String resolvedIdText = referenceResolver.resolveAgentIdTextByCode(agentCode);
        String sql = """
                SELECT a.code AS actor_ref,
                       a.status,
                       COALESCE((
                           SELECT SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE -le.amount END)
                           FROM ledger_entries le
                           WHERE le.account_type = 'AGENT_CASH_CLEARING' AND le.owner_ref = :resolvedIdText
                       ), 0) AS cash_balance,
                       COALESCE((
                           SELECT SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE -le.amount END)
                           FROM ledger_entries le
                           WHERE le.account_type = 'AGENT_WALLET' AND le.owner_ref = :resolvedIdText
                       ), 0) AS commission_balance,
                       (
                           SELECT COUNT(DISTINCT t.id)
                           FROM transactions t
                           JOIN ledger_entries le ON le.transaction_id = t.id
                           WHERE le.account_type IN ('AGENT_CASH_CLEARING', 'AGENT_WALLET')
                             AND le.owner_ref = :resolvedIdText
                             AND t.created_at >= NOW() - INTERVAL '7 days'
                       ) AS tx_count_7d
                FROM agents a
                WHERE a.code = :agentCode
                LIMIT 1
                """;
        var rows = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("agentCode", agentCode)
                        .addValue("resolvedIdText", resolvedIdText), (rs, n) -> new AgentQueryModels.AgentSummary(
                        rs.getString("actor_ref"),
                        rs.getString("status"),
                        rs.getBigDecimal("cash_balance"),
                        rs.getBigDecimal("commission_balance"),
                        rs.getLong("tx_count_7d")));
        return rows.stream().findFirst();
    }

    @Override
    public QueryPage<AgentQueryModels.AgentTransactionItem> listTransactions(String agentCode, AgentQueryModels.AgentTransactionFilter filter) {
        int limit = QueryInputValidator.normalizeLimit(filter.limit(), DEFAULT_LIMIT, MAX_LIMIT);
        boolean desc = QueryInputValidator.resolveSort(filter.sort(), "createdAt");
        QueryInputValidator.validateEnumFilter("type", filter.type());
        QueryInputValidator.validateEnumFilter("status", filter.status());
        CursorPayload cursor = codec.decode(filter.cursor());
        String resolvedIdText = referenceResolver.resolveAgentIdTextByCode(agentCode);

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
                   AND le.account_type IN ('AGENT_CASH_CLEARING', 'AGENT_WALLET')
                   AND le.owner_ref = :resolvedIdText
                )
                """);
        var params = new MapSqlParameterSource("resolvedIdText", resolvedIdText);

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

        if (cursor != null) {
            sql.append(desc
                    ? " AND (t.created_at < :cursorCreatedAt OR (t.created_at = :cursorCreatedAt AND t.id::text < :cursorRef))"
                    : " AND (t.created_at > :cursorCreatedAt OR (t.created_at = :cursorCreatedAt AND t.id::text > :cursorRef))");
            params.addValue("cursorCreatedAt", cursor.createdAt());
            params.addValue("cursorRef", cursor.ref());
        }

        sql.append(" ORDER BY t.created_at ").append(desc ? "DESC" : "ASC").append(", t.id::text ").append(desc ? "DESC" : "ASC");
        sql.append(" LIMIT :limit");
        params.addValue("limit", limit + 1);

        List<AgentQueryModels.AgentTransactionItem> rows = jdbcTemplate.query(sql.toString(), params, (rs, n) -> new AgentQueryModels.AgentTransactionItem(
                rs.getString("transaction_ref"),
                rs.getString("type"),
                rs.getString("status"),
                rs.getBigDecimal("amount"),
                "KMF",
                rs.getTimestamp("created_at").toInstant()
        ));

        boolean hasMore = rows.size() > limit;
        if (hasMore) rows = new ArrayList<>(rows.subList(0, limit));
        String nextCursor = hasMore && !rows.isEmpty()
                ? codec.encode(rows.get(rows.size() - 1).createdAt(), rows.get(rows.size() - 1).transactionRef())
                : null;

        return new QueryPage<>(rows, nextCursor, hasMore);
    }

    @Override
    public QueryPage<AgentQueryModels.AgentActivityItem> listActivities(String agentCode, AgentQueryModels.AgentActivityFilter filter) {
        int limit = QueryInputValidator.normalizeLimit(filter.limit(), DEFAULT_LIMIT, MAX_LIMIT);
        boolean desc = QueryInputValidator.resolveSort(filter.sort(), "occurredAt");
        QueryInputValidator.validateEnumFilter("action", filter.action());
        CursorPayload cursor = codec.decode(filter.cursor());

        StringBuilder sql = new StringBuilder("""
                SELECT id::text AS event_ref, occurred_at, action,
                       metadata_json::jsonb ->> 'resourceType' AS resource_type,
                       metadata_json::jsonb ->> 'resourceRef' AS resource_ref,
                       metadata_json
                FROM audit_events
                WHERE actor_type = 'AGENT'
                  AND actor_id = :actorRef
                """);
        var params = new MapSqlParameterSource("actorRef", agentCode);

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
        if (cursor != null) {
            sql.append(desc
                    ? " AND (occurred_at < :cursorCreatedAt OR (occurred_at = :cursorCreatedAt AND id::text < :cursorRef))"
                    : " AND (occurred_at > :cursorCreatedAt OR (occurred_at = :cursorCreatedAt AND id::text > :cursorRef))");
            params.addValue("cursorCreatedAt", cursor.createdAt());
            params.addValue("cursorRef", cursor.ref());
        }

        sql.append(" ORDER BY occurred_at ").append(desc ? "DESC" : "ASC").append(", id ").append(desc ? "DESC" : "ASC");
        sql.append(" LIMIT :limit");
        params.addValue("limit", limit + 1);

        List<AgentQueryModels.AgentActivityItem> rows = jdbcTemplate.query(sql.toString(), params, (rs, n) -> new AgentQueryModels.AgentActivityItem(
                rs.getString("event_ref"),
                rs.getTimestamp("occurred_at").toInstant(),
                rs.getString("action"),
                rs.getString("resource_type"),
                rs.getString("resource_ref"),
                parseMetadata(rs.getString("metadata_json"))
        ));

        boolean hasMore = rows.size() > limit;
        if (hasMore) rows = new ArrayList<>(rows.subList(0, limit));
        String nextCursor = hasMore && !rows.isEmpty()
                ? codec.encode(rows.get(rows.size() - 1).occurredAt(), rows.get(rows.size() - 1).eventRef())
                : null;

        return new QueryPage<>(rows, nextCursor, hasMore);
    }

    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<>() {});
        }
        catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }
}
