package com.kori.adapters.out.jpa.query.common;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReferenceResolver {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ReferenceResolver(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String resolveAgentIdTextByCode(String agentCode) {
        return resolveIdText("SELECT id::text FROM agents WHERE code = :ref LIMIT 1", agentCode);
    }

    public String resolveMerchantIdTextByCode(String merchantCode) {
        return resolveIdText("SELECT id::text FROM merchants WHERE code = :ref LIMIT 1", merchantCode);
    }

    public String resolveClientIdTextByCode(String clientCode) {
        return resolveIdText("SELECT id::text FROM clients WHERE code = :ref LIMIT 1", clientCode);
    }

    public String resolveTerminalIdTextByUid(String terminalUid) {
        return resolveIdText("SELECT id::text FROM terminals WHERE terminal_uid = :ref LIMIT 1", terminalUid);
    }

    private String resolveIdText(String sql, String ref) {
        return jdbcTemplate.query(sql, new MapSqlParameterSource("ref", ref), rs -> rs.next() ? rs.getString(1) : null);
    }
}
