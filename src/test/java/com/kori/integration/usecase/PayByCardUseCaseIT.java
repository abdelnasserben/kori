package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.AuditEventEntity;
import com.kori.adapters.out.jpa.entity.IdempotencyRecordEntity;
import com.kori.adapters.out.jpa.entity.LedgerEntryEntity;
import com.kori.adapters.out.jpa.entity.TransactionEntity;
import com.kori.application.command.EnrollCardCommand;
import com.kori.application.command.PayByCardCommand;
import com.kori.application.port.in.EnrollCardUseCase;
import com.kori.application.port.in.PayByCardUseCase;
import com.kori.application.result.EnrollCardResult;
import com.kori.application.result.PayByCardResult;
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

class PayByCardUseCaseIT extends AbstractIntegrationTest {

    @Autowired EnrollCardUseCase enrollCardUseCase;
    @Autowired PayByCardUseCase payByCardUseCase;

    @Test
    void happyPath_terminalPaysByCard_createsTx_ledger_audit_and_idempotency() {
        // --- Given (setup via EnrollCard pour avoir client/account/card + PIN hashé)
        String agentId = "AGENT_001";         // seed Flyway
        String terminalId = "TERMINAL_001";   // seed Flyway
        String merchantId = "MERCHANT_001";   // seed Flyway

        String phoneNumber = randomPhone269();
        String cardUid = randomCardUid();
        String pin = "1234";

        EnrollCardResult enrolled = enrollCardUseCase.execute(
                new EnrollCardCommand(
                        idemKey("it-enroll-for-pay"),
                        new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                        phoneNumber,
                        cardUid,
                        pin,
                        agentId
                )
        );
        assertNotNull(enrolled);
        assertNotNull(enrolled.clientId());

        long auditBefore = auditEventJpaRepository.count();

        // Montant choisi pour que fee = 2% => 20.00 (min=10, max=500)
        BigDecimal amount = new BigDecimal("1000.00");
        BigDecimal expectedFee = new BigDecimal("20.00");
        BigDecimal expectedTotalDebited = new BigDecimal("1020.00");

        // --- When
        String payIdempotencyKey = idemKey("it-pay");

        PayByCardResult result = payByCardUseCase.execute(
                new PayByCardCommand(
                        payIdempotencyKey,
                        new ActorContext(ActorType.TERMINAL, "terminal-actor-it", Map.of()),
                        terminalId,
                        cardUid,
                        pin,
                        amount
                )
        );

        // --- Then (business result)
        assertNotNull(result);
        assertNotNull(result.transactionId());
        assertEquals(merchantId, result.merchantId());
        assertEquals(enrolled.clientId(), result.clientId());

        assertEquals(0, result.amount().compareTo(amount));
        assertEquals(0, result.fee().compareTo(expectedFee));
        assertEquals(0, result.totalDebited().compareTo(expectedTotalDebited));

        // --- Then (DB) Transaction
        UUID txId = UUID.fromString(result.transactionId());
        Optional<TransactionEntity> txOpt = transactionJpaRepository.findById(txId);
        assertTrue(txOpt.isPresent(), "Transaction row should exist");
        TransactionEntity tx = txOpt.get();
        assertEquals("PAY_BY_CARD", tx.getType());
        assertEquals(0, tx.getAmount().compareTo(amount));
        assertNotNull(tx.getCreatedAt());
        assertNull(tx.getOriginalTransactionId());

        // --- Then (DB) Ledger entries (3 entries)
        List<LedgerEntryEntity> ledger = ledgerEntryJpaRepository.findByTransactionIdOrderByCreatedAtAscIdAsc(txId);
        assertEquals(3, ledger.size(), "PayByCard should append 3 ledger entries");

        // Asserts "set-like" (robuste sur l'ordre)
        boolean hasClientDebit1020 = ledger.stream().anyMatch(e ->
                "CLIENT".equals(e.getAccount())
                        && "DEBIT".equals(e.getEntryType())
                        && enrolled.clientId().equals(e.getReferenceId())
                        && e.getAmount().compareTo(expectedTotalDebited) == 0
        );
        boolean hasMerchantCredit1000 = ledger.stream().anyMatch(e ->
                "MERCHANT".equals(e.getAccount())
                        && "CREDIT".equals(e.getEntryType())
                        && merchantId.equals(e.getReferenceId())
                        && e.getAmount().compareTo(amount) == 0
        );
        boolean hasPlatformCredit20 = ledger.stream().anyMatch(e ->
                "PLATFORM".equals(e.getAccount())
                        && "CREDIT".equals(e.getEntryType())
                        && e.getReferenceId() == null
                        && e.getAmount().compareTo(expectedFee) == 0
        );

        assertTrue(hasClientDebit1020, "Should debit CLIENT by amount + fee");
        assertTrue(hasMerchantCredit1000, "Should credit MERCHANT by amount");
        assertTrue(hasPlatformCredit20, "Should credit PLATFORM by fee");

        // En bonus: vérifie le netBalance (credit - debit) par compte/ref
        assertEquals(
                0,
                ledgerEntryJpaRepository.netBalance("CLIENT", enrolled.clientId())
                        .compareTo(new BigDecimal("-1020.00"))
        );
        assertEquals(
                0,
                ledgerEntryJpaRepository.netBalance("MERCHANT", merchantId)
                        .compareTo(new BigDecimal("1000.00"))
        );
        // netBalance("PLATFORM", null) n'est pas possible via la query (referenceId = null),
        // donc on valide PLATFORM via hasPlatformCredit20 ci-dessus.

        // --- Then (DB) Audit
        long auditAfter = auditEventJpaRepository.count();
        assertEquals(auditBefore + 1, auditAfter, "One audit event should be written");

        List<AuditEventEntity> audits = auditEventJpaRepository.findAll();
        assertTrue(audits.stream().anyMatch(a ->
                "PAY_BY_CARD".equals(a.getAction())
                        && "TERMINAL".equals(a.getActorType())
                        && "terminal-actor-it".equals(a.getActorId())
                        && a.getMetadataJson() != null
                        && a.getMetadataJson().contains("\"terminalId\":\"" + terminalId + "\"")
                        && a.getMetadataJson().contains("\"merchantId\":\"" + merchantId + "\"")
                        && a.getMetadataJson().contains("\"transactionId\":\"" + result.transactionId() + "\"")
        ), "Audit should contain terminalId, merchantId and transactionId");

        // --- Then (DB) Idempotency
        Optional<IdempotencyRecordEntity> idemOpt = idempotencyJpaRepository.findById(payIdempotencyKey);
        assertTrue(idemOpt.isPresent(), "Idempotency record should be stored");
        IdempotencyRecordEntity idem = idemOpt.get();
        assertEquals(PayByCardResult.class.getName(), idem.getResultType());
        assertNotNull(idem.getResultJson());
        assertFalse(idem.getResultJson().isBlank());
    }
}
