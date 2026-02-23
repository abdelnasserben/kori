package com.kori.adapters.out.jpa.query.bo;

import com.kori.query.model.BackofficeActorDetails;
import com.kori.query.port.out.BackofficeActorDetailReadPort;
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
    public Optional<BackofficeActorDetails> findAgentByRef(String agentCode) {
        return findByCode(agentCode, "agents", "code", "AGENT");
    }

    @Override
    public Optional<BackofficeActorDetails> findClientByRef(String clientCode) {
        return findByCode(clientCode, "clients", "code", "CLIENT");
    }

    @Override
    public Optional<BackofficeActorDetails> findMerchantByRef(String merchantCode) {
        return findByCode(merchantCode, "merchants", "code", "MERCHANT");
    }

    @Override
    public Optional<BackofficeActorDetails> findTerminalByRef(String terminalUid) {
        return findByCode(terminalUid, "terminals", "terminal_uid", "TERMINAL");
    }

    @Override
    public Optional<BackofficeActorDetails> findAdminByRef(String adminUsername) {
        return findByCode(adminUsername, "admins", "username", "ADMIN");
    }

    private Optional<BackofficeActorDetails> findByCode(String actorRef, String table, String codeField, String actorType) {
        String sql = """
                SELECT t.%s AS actor_ref,
                       t.display_name,
                       COALESCE(t.display_name, t.%s) AS display,
                       t.status,
                       t.created_at,
                       (SELECT MAX(ae.occurred_at)
                          FROM audit_events ae
                         WHERE ae.actor_type = :actorType
                           AND ae.actor_id = t.%s) AS last_activity_at
                FROM %s t
                WHERE t.%s = :actorRef
                LIMIT 1
                """.formatted(codeField, codeField, codeField, table, codeField);
        var params = new MapSqlParameterSource().addValue("actorRef", actorRef).addValue("actorType", actorType);
        var rows = jdbcTemplate.query(sql, params, (rs, i) -> new BackofficeActorDetails(
                rs.getString("actor_ref"),
                rs.getString("display_name"),
                rs.getString("display"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("last_activity_at") == null ? null : rs.getTimestamp("last_activity_at").toInstant()
        ));
        return rows.stream().findFirst();
    }
}
