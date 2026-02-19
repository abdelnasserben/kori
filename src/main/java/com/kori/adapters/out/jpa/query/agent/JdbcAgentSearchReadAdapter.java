package com.kori.adapters.out.jpa.query.agent;

import com.kori.query.model.me.AgentQueryModels;
import com.kori.query.port.out.AgentSearchReadPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class JdbcAgentSearchReadAdapter implements AgentSearchReadPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcAgentSearchReadAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<AgentQueryModels.AgentSearchItem> searchByPhone(String phone, int limit) {
        String sql = """
                SELECT c.code AS entity_ref, c.phone_number AS display, c.status
                FROM clients c
                WHERE c.phone_number = :phone OR c.phone_number ILIKE :prefix
                ORDER BY c.phone_number ASC
                LIMIT :limit
                """;
        var params = new MapSqlParameterSource()
                .addValue("phone", phone)
                .addValue("prefix", phone + "%")
                .addValue("limit", limit);

        return jdbcTemplate.query(sql, params, (rs, n) -> new AgentQueryModels.AgentSearchItem(
                "CLIENT",
                rs.getString("entity_ref"),
                rs.getString("display"),
                rs.getString("status"),
                Map.of("client", "/api/v1/clients/" + rs.getString("entity_ref"))
        ));
    }

    @Override
    public List<AgentQueryModels.AgentSearchItem> searchByCardUid(String cardUid, int limit) {
        String sql = """
                SELECT c.card_uid AS entity_ref,
                       c.card_uid AS display,
                       c.status
                FROM cards c
                WHERE c.card_uid = :cardUid OR c.card_uid ILIKE :prefix
                ORDER BY c.card_uid ASC
                LIMIT :limit
                """;
        var params = new MapSqlParameterSource()
                .addValue("cardUid", cardUid)
                .addValue("prefix", cardUid + "%")
                .addValue("limit", limit);

        return jdbcTemplate.query(sql, params, (rs, n) -> new AgentQueryModels.AgentSearchItem(
                "CARD",
                rs.getString("entity_ref"),
                rs.getString("display"),
                rs.getString("status"),
                Map.of("card", "/api/v1/cards/" + rs.getString("entity_ref"))
        ));
    }

    @Override
    public List<AgentQueryModels.AgentSearchItem> searchByTerminalUid(String terminalUid, int limit) {
        String sql = """
                SELECT t.terminal_uid AS entity_ref,
                       t.terminal_uid AS display,
                       t.status,
                       m.code AS merchant_code
                FROM terminals t
                JOIN merchants m ON m.id = t.merchant_id
                WHERE t.terminal_uid = :terminalUid OR t.terminal_uid ILIKE :prefix
                ORDER BY t.terminal_uid ASC
                LIMIT :limit
                """;
        var params = new MapSqlParameterSource()
                .addValue("terminalUid", terminalUid)
                .addValue("prefix", terminalUid + "%")
                .addValue("limit", limit);

        return jdbcTemplate.query(sql, params, (rs, n) -> new AgentQueryModels.AgentSearchItem(
                "TERMINAL",
                rs.getString("entity_ref"),
                rs.getString("display"),
                rs.getString("status"),
                Map.of(
                        "terminal", "/api/v1/terminals/" + rs.getString("entity_ref"),
                        "merchant", "/api/v1/merchants/" + rs.getString("merchant_code")
                )
        ));
    }
}
