package com.kori.adapters.out.jpa.query.bo;

import com.kori.query.model.BackofficeLookupItem;
import com.kori.query.model.BackofficeLookupQuery;
import com.kori.query.port.out.BackofficeLookupReadPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JdbcBackofficeLookupReadAdapter implements BackofficeLookupReadPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcBackofficeLookupReadAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<BackofficeLookupItem> search(BackofficeLookupQuery query) {
        String sql = """
                SELECT * FROM (
                    SELECT 'CLIENT' AS entity_type, c.code AS entity_ref, COALESCE(c.display_name, c.code) AS display, c.status,
                    CASE WHEN c.code = :exactQ OR c.phone_number = :exactQ THEN 0 ELSE 1 END AS rank
                    FROM clients c
                    WHERE (:type IS NULL OR :type = 'CLIENT_CODE')
                       AND (c.code = :exactQ OR c.code ILIKE :containsQ OR c.phone_number = :exactQ
                       OR c.phone_number ILIKE :containsQ OR c.display_name ILIKE :containsQ)

                    UNION ALL

                    SELECT 'CARD' AS entity_type,
                    cd.card_uid AS entity_ref,
                    cd.card_uid AS display,
                    cd.status,
                    CASE WHEN cd.card_uid = :exactQ THEN 0 ELSE 1 END AS rank
                    FROM cards cd
                    WHERE (:type IS NULL OR :type = 'CARD_UID')
                      AND (cd.card_uid = :exactQ OR cd.card_uid ILIKE :containsQ)

                    UNION ALL

                    SELECT 'TERMINAL' AS entity_type,
                    t.terminal_uid AS entity_ref,
                    COALESCE(t.display_name, t.terminal_uid) AS display,
                    t.status,
                    CASE WHEN t.terminal_uid = :exactQ THEN 0 ELSE 1 END AS rank
                    FROM terminals t
                    WHERE (:type IS NULL OR :type = 'TERMINAL_UID')
                      AND (t.terminal_uid = :exactQ OR t.terminal_uid ILIKE :containsQ OR t.display_name ILIKE :containsQ)

                    UNION ALL

                    SELECT 'TRANSACTION' AS entity_type,
                    tx.id::text AS entity_ref,
                    tx.id::text AS display,
                    COALESCE(p.status, cr.status, 'COMPLETED') AS status,
                    CASE WHEN tx.id::text = :exactQ THEN 0 ELSE 1 END AS rank
                    FROM transactions tx
                    LEFT JOIN payouts p ON p.transaction_id = tx.id
                    LEFT JOIN client_refunds cr ON cr.transaction_id = tx.id
                    WHERE (:type IS NULL OR :type = 'TRANSACTION_REF')
                      AND (tx.id::text = :exactQ OR tx.id::text ILIKE :containsQ)

                    UNION ALL

                    SELECT 'MERCHANT' AS entity_type,
                    m.code AS entity_ref,
                    COALESCE(m.display_name, m.code) AS display,
                    m.status,
                    CASE WHEN m.code = :exactQ THEN 0 ELSE 1 END AS rank
                    FROM merchants m
                    WHERE (:type IS NULL OR :type = 'MERCHANT_CODE')
                      AND (m.code = :exactQ OR m.code ILIKE :containsQ OR m.display_name ILIKE :containsQ)

                    UNION ALL

                    SELECT 'AGENT' AS entity_type,
                    a.code AS entity_ref,
                    COALESCE(a.display_name, a.code) AS display, 
                    a.status,
                    CASE WHEN a.code = :exactQ THEN 0 ELSE 1 END AS rank
                    FROM agents a
                    WHERE (:type IS NULL OR :type = 'AGENT_CODE')
                      AND (a.code = :exactQ OR a.code ILIKE :containsQ OR a.display_name ILIKE :containsQ)
                      
                    UNION ALL
                    
                    SELECT 'ADMIN' AS entity_type,
                    ad.username AS entity_ref,
                    COALESCE(ad.display_name, ad.username) AS display, 
                    ad.status,
                    CASE WHEN ad.username = :exactQ THEN 0 ELSE 1 END AS rank
                    FROM admins ad
                    WHERE (:type IS NULL OR :type = 'ADMIN_USERNAME')
                      AND (ad.username = :exactQ OR ad.username ILIKE :containsQ OR ad.display_name ILIKE :containsQ)
                ) lookups
                ORDER BY rank ASC, entity_type ASC, display ASC
                LIMIT :limit
                """;

        var params = new MapSqlParameterSource()
                .addValue("type", query.type() == null || query.type().isBlank() ? null : query.type())
                .addValue("exactQ", query.q())
                .addValue("containsQ", "%" + query.q() + "%")
                .addValue("limit", query.limit());

        return jdbcTemplate.query(sql, params, (rs, i) -> new BackofficeLookupItem(
                rs.getString("entity_type"),
                rs.getString("entity_ref"),
                rs.getString("display"),
                rs.getString("status")
        ));
    }
}
