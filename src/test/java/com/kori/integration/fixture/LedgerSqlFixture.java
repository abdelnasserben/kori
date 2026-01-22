package com.kori.integration.fixture;

import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Fixture SQL dédiée à "ledger_entries" (append-only).
 *
 * Schéma (V1__init.sql):
 * - id (UUID PK)
 * - transaction_id (UUID FK -> transactions.id)
 * - account (VARCHAR(64))
 * - entry_type (CREDIT|DEBIT)
 * - amount (NUMERIC(19,2))
 * - reference_id (VARCHAR(128), nullable)
 * - created_at (TIMESTAMPTZ, default now())
 *
 * Important: le ledger est append-only (triggers empêchent UPDATE/DELETE).
 */
public final class LedgerSqlFixture {

    private final JdbcTemplate jdbcTemplate;

    public LedgerSqlFixture(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID insertCredit(UUID transactionId, String account, BigDecimal amount) {
        return insertEntry(UUID.randomUUID(), transactionId, account, "CREDIT", amount, null, null);
    }

    public UUID insertDebit(UUID transactionId, String account, BigDecimal amount) {
        return insertEntry(UUID.randomUUID(), transactionId, account, "DEBIT", amount, null, null);
    }

    public UUID insertCredit(UUID transactionId, String account, BigDecimal amount, String referenceId) {
        return insertEntry(UUID.randomUUID(), transactionId, account, "CREDIT", amount, referenceId, null);
    }

    public UUID insertDebit(UUID transactionId, String account, BigDecimal amount, String referenceId) {
        return insertEntry(UUID.randomUUID(), transactionId, account, "DEBIT", amount, referenceId, null);
    }

    public UUID insertEntry(UUID entryId,
                            UUID transactionId,
                            String account,
                            String entryType, // "CREDIT" | "DEBIT"
                            BigDecimal amount,
                            String referenceId,
                            OffsetDateTime createdAt) {

        if (!"CREDIT".equals(entryType) && !"DEBIT".equals(entryType)) {
            throw new IllegalArgumentException("entryType must be CREDIT or DEBIT");
        }

        if (createdAt == null) {
            jdbcTemplate.update(
                    """
                    INSERT INTO ledger_entries (id, transaction_id, account, entry_type, amount, reference_id)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    entryId, transactionId, account, entryType, amount, referenceId
            );
        } else {
            jdbcTemplate.update(
                    """
                    INSERT INTO ledger_entries (id, transaction_id, account, entry_type, amount, reference_id, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    entryId, transactionId, account, entryType, amount, referenceId, createdAt
            );
        }

        return entryId;
    }

    public int countByTransaction(UUID transactionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ledger_entries WHERE transaction_id = ?",
                Integer.class,
                transactionId
        );
        return count == null ? 0 : count;
    }

    public List<UUID> findIdsByTransaction(UUID transactionId) {
        return jdbcTemplate.query(
                "SELECT id FROM ledger_entries WHERE transaction_id = ? ORDER BY created_at ASC",
                (rs, rowNum) -> (UUID) rs.getObject("id"),
                transactionId
        );
    }

    public BigDecimal sumByTransactionAndType(UUID transactionId, String entryType) {
        if (!"CREDIT".equals(entryType) && !"DEBIT".equals(entryType)) {
            throw new IllegalArgumentException("entryType must be CREDIT or DEBIT");
        }

        BigDecimal sum = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(amount), 0)
                FROM ledger_entries
                WHERE transaction_id = ? AND entry_type = ?
                """,
                BigDecimal.class,
                transactionId, entryType
        );
        return sum == null ? BigDecimal.ZERO : sum;
    }
}
