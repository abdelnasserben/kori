package com.kori.integration.fixture;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;
import java.util.UUID;

/**
 * Fixture SQL dédiée à la table "clients".
 *
 * Objectifs :
 * - Créer rapidement un client (sans passer par JPA) pour les tests d'intégration
 * - Éviter la duplication de SQL dans les tests
 * - Rester strictement conforme au schéma DB existant (clients: id, phone_number, status)
 *
 * Note: les tests étant @Transactional, tout est rollback automatiquement.
 */
public final class ClientSqlFixture {

    private final JdbcTemplate jdbcTemplate;

    public ClientSqlFixture(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID insertActiveClient(String phoneNumber) {
        return insertClient(UUID.randomUUID(), phoneNumber, "ACTIVE");
    }

    public UUID insertClient(UUID clientId, String phoneNumber, String status) {
        jdbcTemplate.update(
                """
                INSERT INTO clients (id, phone_number, status)
                VALUES (?, ?, ?)
                """,
                clientId, phoneNumber, status
        );
        return clientId;
    }

    public void updateStatus(UUID clientId, String status) {
        jdbcTemplate.update(
                "UPDATE clients SET status = ? WHERE id = ?",
                status, clientId
        );
    }

    public Optional<UUID> findIdByPhone(String phoneNumber) {
        // queryForObject peut lever une exception si 0 row -> on encapsule proprement
        try {
            UUID id = jdbcTemplate.queryForObject(
                    "SELECT id FROM clients WHERE phone_number = ?",
                    (rs, rowNum) -> (UUID) rs.getObject("id"),
                    phoneNumber
            );
            return Optional.ofNullable(id);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existsByPhone(String phoneNumber) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clients WHERE phone_number = ?",
                Integer.class,
                phoneNumber
        );
        return count != null && count > 0;
    }
}
