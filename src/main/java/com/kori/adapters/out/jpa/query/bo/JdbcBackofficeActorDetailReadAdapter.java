package com.kori.adapters.out.jpa.query.bo;

import com.kori.application.port.out.query.BackofficeActorDetailReadPort;
import com.kori.application.query.BackofficeActorDetails;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JdbcBackofficeActorDetailReadAdapter implements BackofficeActorDetailReadPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcBackofficeActorDetailReadAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<BackofficeActorDetails> findAgentById(String agentId) {
        return findById(agentId, "agents", "code", "AGENT");
    }

    @Override
    public Optional<BackofficeActorDetails> findClientById(String clientId) {
        return findById(clientId, "clients", "phone_number", "CLIENT");
    }

    @Override
    public Optional<BackofficeActorDetails> findMerchantById(String merchantId) {
        return findById(merchantId, "merchants", "code", "MERCHANT");
    }

    private Optional<BackofficeActorDetails> findById(String actorId, String table, String displayField, String actorType) {
        String sql = """
                SELECT t.id::text AS actor_id,
                       t.%s AS display,
                       t.status,
                       t.created_at,
                       (SELECT MAX(ae.occurred_at)
                          FROM audit_events ae
                         WHERE ae.actor_type = :actorType
                           AND ae.actor_id = t.id::text) AS last_activity_at
                FROM %s t
                WHERE t.id = CAST(:actorId AS uuid)
                LIMIT 1
                """.formatted(displayField, table);
        var params = new MapSqlParameterSource()
                .addValue("actorId", actorId)
                .addValue("actorType", actorType);
        var rows = jdbcTemplate.query(sql, params, (rs, i) -> new BackofficeActorDetails(
                rs.getString("actor_id"),
                rs.getString("display"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("last_activity_at") == null ? null : rs.getTimestamp("last_activity_at").toInstant()
        ));
        return rows.stream().findFirst();
    }
}
