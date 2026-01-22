package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.AuditEventEntity;
import com.kori.adapters.out.jpa.entity.IdempotencyRecordEntity;
import com.kori.adapters.out.jpa.entity.LedgerEntryEntity;
import com.kori.adapters.out.jpa.entity.TransactionEntity;
import com.kori.application.command.EnrollCardCommand;
import com.kori.application.command.PayByCardCommand;
import com.kori.application.command.ReversalCommand;
import com.kori.application.port.in.EnrollCardUseCase;
import com.kori.application.port.in.PayByCardUseCase;
import com.kori.application.port.in.ReversalUseCase;
import com.kori.application.result.PayByCardResult;
import com.kori.application.result.ReversalResult;
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

class ReversalUseCaseIT extends AbstractIntegrationTest {

    @Autowired EnrollCardUseCase enrollCardUseCase;
    @Autowired PayByCardUseCase payByCardUseCase;
    @Autowired ReversalUseCase reversalUseCase;

    @Test
    void happyPath_adminReversesPayByCard_createsReverseTx_ledger_audit_and_idempotency() {
        // --- Given: a PAY_BY_CARD transaction exists
        String agentId = "AGENT_001";
        String terminalId = "TERMINAL_001";
        String merchantId = "MERCHANT_001";

        String phoneNumber = randomPhone269();
        String cardUid = randomCardUid();
        String pin = "1234";

        enrollCardUseCase.execute(new EnrollCardCommand(
                idemKey("it-enroll-for-reversal"),
                new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                phoneNumber,
                cardUid,
                pin,
                agentId
        ));

        // fee_config => card_payment_fee_rate = 2%, min=10, max=500
        BigDecimal amount = new BigDecimal("1000.00");
        BigDecimal expectedFee = new BigDecimal("20.00");
        BigDecimal expectedTotalDebited = new BigDecimal("1020.00");

        PayByCardResult payResult = payByCardUseCase.execute(new PayByCardCommand(
                idemKey("it-pay-for-reversal"),
                new ActorContext(ActorType.TERMINAL, "terminal-actor-it", Map.of()),
                terminalId,
                cardUid,
                pin,
                amount
        ));

        UUID originalTxId = UUID.fromString(payResult.transactionId());

        // Capture balances BEFORE reversal (only accounts with non-null referenceId)
        BigDecimal clientBalanceBefore =
                ledgerEntryJpaRepository.netBalance("CLIENT", payResult.clientId());
        BigDecimal merchantBalanceBefore =
                ledgerEntryJpaRepository.netBalance("MERCHANT", merchantId);

        long auditBefore = auditEventJpaRepository.count();

        // --- When: ADMIN reverses the transaction
        String reversalIdempotencyKey = idemKey("it-reversal");

        ReversalResult reversalResult = reversalUseCase.execute(new ReversalCommand(
                reversalIdempotencyKey,
                new ActorContext(ActorType.ADMIN, "admin-actor-it", Map.of()),
                originalTxId.toString()
        ));

        // --- Then (business result)
        assertNotNull(reversalResult);
        assertEquals(originalTxId.toString(), reversalResult.originalTransactionId());
        assertNotNull(reversalResult.reversalTransactionId());

        UUID reversalTxId = UUID.fromString(reversalResult.reversalTransactionId());

        // --- Then (DB) Reversal transaction exists and references original
        Optional<TransactionEntity> reversalTxOpt = transactionJpaRepository.findById(reversalTxId);
        assertTrue(reversalTxOpt.isPresent(), "Reversal transaction should exist");

        TransactionEntity reversalTx = reversalTxOpt.get();
        assertEquals("REVERSAL", reversalTx.getType());
        assertEquals(originalTxId, reversalTx.getOriginalTransactionId());
        assertNotNull(reversalTx.getCreatedAt());

        // --- Then (DB) Ledger entries for reversal = inverse of original entries
        List<LedgerEntryEntity> reversalLedger =
                ledgerEntryJpaRepository.findByTransactionIdOrderByCreatedAtAscIdAsc(reversalTxId);
        assertEquals(3, reversalLedger.size(), "Reversal should invert PAY_BY_CARD 3 ledger entries");

        boolean hasClientCreditTotalDebited = reversalLedger.stream().anyMatch(e ->
                "CLIENT".equals(e.getAccount())
                        && "CREDIT".equals(e.getEntryType())
                        && payResult.clientId().equals(e.getReferenceId())
                        && e.getAmount().compareTo(expectedTotalDebited) == 0
        );

        boolean hasMerchantDebitAmount = reversalLedger.stream().anyMatch(e ->
                "MERCHANT".equals(e.getAccount())
                        && "DEBIT".equals(e.getEntryType())
                        && merchantId.equals(e.getReferenceId())
                        && e.getAmount().compareTo(amount) == 0
        );

        boolean hasPlatformDebitFee = reversalLedger.stream().anyMatch(e ->
                "PLATFORM".equals(e.getAccount())
                        && "DEBIT".equals(e.getEntryType())
                        && e.getReferenceId() == null
                        && e.getAmount().compareTo(expectedFee) == 0
        );

        assertTrue(hasClientCreditTotalDebited, "Should credit CLIENT by total debited amount");
        assertTrue(hasMerchantDebitAmount, "Should debit MERCHANT by amount");
        assertTrue(hasPlatformDebitFee, "Should debit PLATFORM by fee");

        // --- Then (delta balances) - only for accounts with referenceId non-null
        BigDecimal clientBalanceAfter =
                ledgerEntryJpaRepository.netBalance("CLIENT", payResult.clientId());
        BigDecimal merchantBalanceAfter =
                ledgerEntryJpaRepository.netBalance("MERCHANT", merchantId);

        assertEquals(
                0,
                clientBalanceAfter.subtract(clientBalanceBefore).compareTo(expectedTotalDebited),
                "Client should recover total debited amount"
        );

        assertEquals(
                0,
                merchantBalanceAfter.subtract(merchantBalanceBefore).compareTo(amount.negate()),
                "Merchant should lose the paid amount"
        );

        // --- Then (audit)
        long auditAfter = auditEventJpaRepository.count();
        assertEquals(auditBefore + 1, auditAfter);

        List<AuditEventEntity> audits = auditEventJpaRepository.findAll();
        assertTrue(audits.stream().anyMatch(a ->
                "REVERSAL".equals(a.getAction())
                        && "ADMIN".equals(a.getActorType())
                        && "admin-actor-it".equals(a.getActorId())
                        && a.getMetadataJson() != null
                        && a.getMetadataJson().contains("\"transactionId\":\"" + reversalTxId + "\"")
                        && a.getMetadataJson().contains("\"originalTransactionId\":\"" + originalTxId + "\"")
        ));

        // --- Then (idempotency)
        Optional<IdempotencyRecordEntity> idemOpt =
                idempotencyJpaRepository.findById(reversalIdempotencyKey);
        assertTrue(idemOpt.isPresent());

        IdempotencyRecordEntity idem = idemOpt.get();
        assertEquals(ReversalResult.class.getName(), idem.getResultType());
        assertNotNull(idem.getResultJson());
        assertFalse(idem.getResultJson().isBlank());
    }
}
