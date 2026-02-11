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
                SELECT *
                FROM (
                    SELECT 'CLIENT' AS entity_type,
                           c.id::text AS entity_id,
                           c.phone_number AS display,
                           c.status,
                           CASE WHEN c.phone_number = :exactQ THEN 0 ELSE 1 END AS rank
                    FROM clients c
                    WHERE (:type IS NULL OR :type = 'CLIENT_PHONE')
                      AND (c.phone_number = :exactQ OR c.phone_number ILIKE :containsQ)

                    UNION ALL

                    SELECT 'CARD' AS entity_type,
                           cd.id::text AS entity_id,
                           cd.card_uid AS display,
                           cd.status,
                           CASE WHEN cd.card_uid = :exactQ THEN 0 ELSE 1 END AS rank
                    FROM cards cd
                    WHERE (:type IS NULL OR :type = 'CARD_UID')
                      AND (cd.card_uid = :exactQ OR cd.card_uid ILIKE :containsQ)

                    UNION ALL

                    SELECT 'TERMINAL' AS entity_type,
                           t.id::text AS entity_id,
                           t.id::text AS display,
                           t.status,
                           CASE WHEN t.id::text = :exactQ THEN 0 ELSE 1 END AS rank
                    FROM terminals t
                    WHERE (:type IS NULL OR :type = 'TERMINAL_ID')
                      AND (t.id::text = :exactQ OR t.id::text ILIKE :containsQ)

                    UNION ALL

                    SELECT 'TRANSACTION' AS entity_type,
                           tx.id::text AS entity_id,
                           tx.id::text AS display,
                           COALESCE(p.status, cr.status, 'COMPLETED') AS status,
                           CASE WHEN tx.id::text = :exactQ THEN 0 ELSE 1 END AS rank
                    FROM transactions tx
                    LEFT JOIN payouts p ON p.transaction_id = tx.id
                    LEFT JOIN client_refunds cr ON cr.transaction_id = tx.id
                    WHERE (:type IS NULL OR :type = 'TRANSACTION_ID')
                      AND (tx.id::text = :exactQ OR tx.id::text ILIKE :containsQ)

                    UNION ALL

                    SELECT 'MERCHANT' AS entity_type,
                           m.id::text AS entity_id,
                           m.code AS display,
                           m.status,
                           CASE WHEN m.code = :exactQ THEN 0 ELSE 1 END AS rank
                    FROM merchants m
                    WHERE (:type IS NULL OR :type = 'MERCHANT_CODE')
                      AND (m.code = :exactQ OR m.code ILIKE :containsQ)

                    UNION ALL

                    SELECT 'AGENT' AS entity_type,
                           a.id::text AS entity_id,
                           a.code AS display,
                           a.status,
                           CASE WHEN a.code = :exactQ THEN 0 ELSE 1 END AS rank
                    FROM agents a
                    WHERE (:type IS NULL OR :type = 'AGENT_CODE')
                      AND (a.code = :exactQ OR a.code ILIKE :containsQ)
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
                rs.getString("entity_id"),
                rs.getString("display"),
                rs.getString("status")
        ));
    }
}
