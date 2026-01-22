package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.*;
import com.kori.application.command.EnrollCardCommand;
import com.kori.application.port.in.EnrollCardUseCase;
import com.kori.application.result.EnrollCardResult;
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

class EnrollCardUseCaseIT extends AbstractIntegrationTest {

    @Autowired EnrollCardUseCase enrollCardUseCase;

    @Test
    void happyPath_createsClientAccountCard_tx_ledger_audit_and_idempotency() {
        // Given
        String idempotencyKey = idemKey("it-enroll");
        String phoneNumber = randomPhone269();
        String cardUid = randomCardUid();
        String pin = "1234";

        // Seeded by Flyway V3__seed_minimal_references.sql
        String agentId = "AGENT_001";

        long auditBefore = auditEventJpaRepository.count();

        EnrollCardCommand cmd = new EnrollCardCommand(
                idempotencyKey,
                new ActorContext(ActorType.AGENT, "agent-actor-1", Map.of()),
                phoneNumber,
                cardUid,
                pin,
                agentId
        );

        // When
        EnrollCardResult result = enrollCardUseCase.execute(cmd);

        // Then (business result)
        assertNotNull(result);
        assertNotNull(result.transactionId());
        assertNotNull(result.clientId());
        assertNotNull(result.accountId());
        assertNotNull(result.cardId());

        assertTrue(result.clientCreated(), "Client should be created for a new phone number");
        assertTrue(result.accountCreated(), "Account should be created for a new client");

        assertEquals(new BigDecimal("500.00"), result.cardPrice());
        assertEquals(new BigDecimal("200.00"), result.agentCommission());

        // Then (DB state) - Client
        Optional<ClientEntity> clientOpt = clientJpaRepository.findByPhoneNumber(phoneNumber);
        assertTrue(clientOpt.isPresent(), "Client row should exist");
        ClientEntity client = clientOpt.get();
        assertEquals("ACTIVE", client.getStatus());

        // Account
        Optional<AccountEntity> accountOpt = accountJpaRepository.findByClientId(client.getId());
        assertTrue(accountOpt.isPresent(), "Account row should exist for client");
        AccountEntity account = accountOpt.get();
        assertEquals("ACTIVE", account.getStatus());

        // Card
        Optional<CardEntity> cardOpt = cardJpaRepository.findByCardUid(cardUid);
        assertTrue(cardOpt.isPresent(), "Card row should exist");
        CardEntity card = cardOpt.get();
        assertEquals(account.getId(), card.getAccountId());
        assertEquals("ACTIVE", card.getStatus());
        assertEquals(0, card.getFailedPinAttempts());
        assertNotNull(card.getPin());
        assertFalse(card.getPin().isBlank(), "Hashed pin should be stored");

        // Transaction
        UUID txId = UUID.fromString(result.transactionId());
        Optional<TransactionEntity> txOpt = transactionJpaRepository.findById(txId);
        assertTrue(txOpt.isPresent(), "Transaction row should exist");
        TransactionEntity tx = txOpt.get();
        assertEquals("ENROLL_CARD", tx.getType());
        assertEquals(0, tx.getAmount().compareTo(new BigDecimal("500.00")));
        assertNotNull(tx.getCreatedAt());
        assertNull(tx.getOriginalTransactionId());

        // Ledger entries (2 credits expected)
        List<LedgerEntryEntity> ledger = ledgerEntryJpaRepository.findByTransactionIdOrderByCreatedAtAscIdAsc(txId);
        assertEquals(2, ledger.size(), "EnrollCard should append 2 ledger entries");

        // Validate as a set (no ordering assumption)
        boolean hasPlatformCredit300 = ledger.stream().anyMatch(e ->
                "PLATFORM".equals(e.getAccount())
                        && "CREDIT".equals(e.getEntryType())
                        && e.getReferenceId() == null
                        && e.getAmount().compareTo(new BigDecimal("300.00")) == 0
        );
        boolean hasAgentCredit200 = ledger.stream().anyMatch(e ->
                "AGENT".equals(e.getAccount())
                        && "CREDIT".equals(e.getEntryType())
                        && agentId.equals(e.getReferenceId())
                        && e.getAmount().compareTo(new BigDecimal("200.00")) == 0
        );

        assertTrue(hasPlatformCredit300, "Should credit PLATFORM with (price - commission) = 300.00");
        assertTrue(hasAgentCredit200, "Should credit AGENT with commission = 200.00");

        // Audit (1 event appended)
        long auditAfter = auditEventJpaRepository.count();
        assertEquals(auditBefore + 1, auditAfter, "One audit event should be written");

        List<AuditEventEntity> audits = auditEventJpaRepository.findAll();
        assertTrue(audits.stream().anyMatch(a ->
                "ENROLL_CARD".equals(a.getAction())
                        && "AGENT".equals(a.getActorType())
                        && "agent-actor-1".equals(a.getActorId())
                        && a.getMetadataJson() != null
                        && a.getMetadataJson().contains("\"agentId\":\"" + agentId + "\"")
                        && a.getMetadataJson().contains("\"transactionId\":\"" + result.transactionId() + "\"")
        ), "Audit event should contain agentId and transactionId");

        // Idempotency record
        Optional<IdempotencyRecordEntity> idemOpt = idempotencyJpaRepository.findById(idempotencyKey);
        assertTrue(idemOpt.isPresent(), "Idempotency record should be stored");
        IdempotencyRecordEntity idem = idemOpt.get();
        assertEquals(EnrollCardResult.class.getName(), idem.getResultType());
        assertNotNull(idem.getResultJson());
        assertFalse(idem.getResultJson().isBlank());
    }
}
