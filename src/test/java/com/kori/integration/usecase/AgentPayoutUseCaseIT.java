package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.AuditEventEntity;
import com.kori.adapters.out.jpa.entity.IdempotencyRecordEntity;
import com.kori.adapters.out.jpa.entity.LedgerEntryEntity;
import com.kori.adapters.out.jpa.entity.TransactionEntity;
import com.kori.application.command.AgentPayoutCommand;
import com.kori.application.command.EnrollCardCommand;
import com.kori.application.port.in.AgentPayoutUseCase;
import com.kori.application.port.in.EnrollCardUseCase;
import com.kori.application.result.AgentPayoutResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AgentPayoutUseCaseIT extends AbstractIntegrationTest {

    @Autowired AgentPayoutUseCase agentPayoutUseCase;
    @Autowired EnrollCardUseCase enrollCardUseCase;

    @Test
    void happyPath_adminInitiatesAgentPayout_paysExactDue_and_bringsAgentBalanceToZero() {
        // Given
        String agentId = "AGENT_001";

        // --- Calculate agent due BEFORE payout (mutable)
        BigDecimal computedDue = ledgerEntryJpaRepository.netBalance("AGENT", agentId);
        if (computedDue == null) {
            computedDue = BigDecimal.ZERO;
        }

        // Ensure due > 0, otherwise create commission via EnrollCard
        if (computedDue.compareTo(BigDecimal.ZERO) == 0) {
            enrollCardUseCase.execute(new EnrollCardCommand(
                    idemKey("it-enroll-for-payout"),
                    new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                    randomPhone269(),
                    randomCardUid(),
                    "1234",
                    agentId
            ));

            computedDue = ledgerEntryJpaRepository.netBalance("AGENT", agentId);
            if (computedDue == null) {
                computedDue = BigDecimal.ZERO;
            }

            assertTrue(computedDue.compareTo(BigDecimal.ZERO) > 0,
                    "After enrollment, agent should have a positive due");
        }

        // --- Snapshot FINAL for lambdas & assertions
        final BigDecimal dueBefore = computedDue;

        long auditBeforeCount = auditEventJpaRepository.count();
        String idempotencyKey = idemKey("it-agent-payout");

        // NOTE: command.amount is ignored by the use case
        AgentPayoutCommand cmd = new AgentPayoutCommand(
                idempotencyKey,
                new ActorContext(ActorType.ADMIN, "admin-actor-it", Map.of()),
                agentId
        );

        // When
        AgentPayoutResult result = agentPayoutUseCase.execute(cmd);

        // Then (business result)
        assertNotNull(result);
        assertNotNull(result.transactionId());
        assertNotNull(result.payoutId());
        assertEquals(agentId, result.agentId());
        assertEquals(0, result.amount().compareTo(dueBefore));
        assertEquals("COMPLETED", result.payoutStatus().name());

        // Then (DB) Transaction
        UUID txId = UUID.fromString(result.transactionId());
        Optional<TransactionEntity> txOpt = transactionJpaRepository.findById(txId);
        assertTrue(txOpt.isPresent(), "Transaction row should exist");
        TransactionEntity tx = txOpt.get();
        assertEquals("AGENT_PAYOUT", tx.getType());
        assertEquals(0, tx.getAmount().compareTo(dueBefore));
        assertNotNull(tx.getCreatedAt());
        assertNull(tx.getOriginalTransactionId());

        // Then (DB) Ledger entries (2 entries)
        List<LedgerEntryEntity> ledger =
                ledgerEntryJpaRepository.findByTransactionIdOrderByCreatedAtAscIdAsc(txId);
        assertEquals(2, ledger.size(), "AgentPayout should append 2 ledger entries");

        boolean hasAgentDebitDue = ledger.stream().anyMatch(e ->
                "AGENT".equals(e.getAccount())
                        && "DEBIT".equals(e.getEntryType())
                        && agentId.equals(e.getReferenceId())
                        && e.getAmount().compareTo(dueBefore) == 0
        );

        boolean hasPlatformClearingCreditDue = ledger.stream().anyMatch(e ->
                "PLATFORM_CLEARING".equals(e.getAccount())
                        && "CREDIT".equals(e.getEntryType())
                        && e.getReferenceId() == null
                        && e.getAmount().compareTo(dueBefore) == 0
        );

        assertTrue(hasAgentDebitDue, "Should debit AGENT by due");
        assertTrue(hasPlatformClearingCreditDue, "Should credit PLATFORM_CLEARING by due");

        // Then: agent balance should be ZERO after payout
        BigDecimal dueAfter = ledgerEntryJpaRepository.netBalance("AGENT", agentId);
        if (dueAfter == null) {
            dueAfter = BigDecimal.ZERO;
        }

        assertEquals(0, dueAfter.compareTo(BigDecimal.ZERO),
                "After payout, agent due should be zero");

        // Audit
        long auditAfterCount = auditEventJpaRepository.count();
        assertEquals(auditBeforeCount + 1, auditAfterCount);

        List<AuditEventEntity> audits = auditEventJpaRepository.findAll();
        assertTrue(audits.stream().anyMatch(a ->
                "AGENT_PAYOUT".equals(a.getAction())
                        && "ADMIN".equals(a.getActorType())
                        && "admin-actor-it".equals(a.getActorId())
                        && a.getMetadataJson() != null
                        && a.getMetadataJson().contains("\"agentId\":\"" + agentId + "\"")
                        && a.getMetadataJson().contains("\"transactionId\":\"" + result.transactionId() + "\"")
                        && a.getMetadataJson().contains("\"payoutId\":\"" + result.payoutId() + "\"")
        ));

        // Idempotency
        Optional<IdempotencyRecordEntity> idemOpt =
                idempotencyJpaRepository.findById(idempotencyKey);
        assertTrue(idemOpt.isPresent());
        IdempotencyRecordEntity idem = idemOpt.get();
        assertEquals(AgentPayoutResult.class.getName(), idem.getResultType());
        assertNotNull(idem.getResultJson());
        assertFalse(idem.getResultJson().isBlank());
    }
}
