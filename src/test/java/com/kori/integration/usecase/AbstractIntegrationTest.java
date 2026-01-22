package com.kori.integration;

import com.kori.adapters.out.jpa.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Socle commun pour tous les tests d'intégration application-layer.
 *
 * - Charge le contexte Spring complet (@SpringBootTest)
 * - Exécute chaque test dans une transaction rollbackée automatiquement (@Transactional)
 * - Expose JdbcTemplate + repositories communs
 * - Fournit quelques helpers simples (UUID, phone, etc.)
 */
@SpringBootTest
@Transactional
public abstract class AbstractIntegrationTest {

    // --- Low-level SQL helper (fixtures SQL / assertions)
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    // --- Common repositories (Spring Data JPA)
    @Autowired protected ClientJpaRepository clientJpaRepository;
    @Autowired protected AccountJpaRepository accountJpaRepository;
    @Autowired protected CardJpaRepository cardJpaRepository;

    @Autowired protected TransactionJpaRepository transactionJpaRepository;
    @Autowired protected LedgerEntryJpaRepository ledgerEntryJpaRepository;
    @Autowired protected AuditEventJpaRepository auditEventJpaRepository;
    @Autowired protected IdempotencyJpaRepository idempotencyJpaRepository;

    // --- Reference data repositories (useful for fixtures / asserts)
    @Autowired protected AgentJpaRepository agentJpaRepository;
    @Autowired protected MerchantJpaRepository merchantJpaRepository;
    @Autowired protected TerminalJpaRepository terminalJpaRepository;

    // --- Other repositories that can be handy for integration fixtures
    @Autowired protected FeeConfigJpaRepository feeConfigJpaRepository;
    @Autowired protected CommissionConfigJpaRepository commissionConfigJpaRepository;
    @Autowired protected SecurityConfigJpaRepository securityConfigJpaRepository;
    @Autowired protected PayoutJpaRepository payoutJpaRepository;

    // -----------------
    // Helpers
    // -----------------

    protected UUID uuid() {
        return UUID.randomUUID();
    }

    protected String idemKey(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    /**
     * Téléphone "safe" (format E.164) pour éviter collisions entre tests.
     * Ex: +269700123456
     */
    protected String randomPhone269() {
        int suffix = 100000 + (int) (Math.random() * 900000);
        return "+269700" + suffix;
    }

    protected String randomCardUid() {
        return "CARD-UID-" + UUID.randomUUID();
    }
}
