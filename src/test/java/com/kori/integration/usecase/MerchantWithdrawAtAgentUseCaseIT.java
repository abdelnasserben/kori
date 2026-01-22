package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.AuditEventEntity;
import com.kori.adapters.out.jpa.entity.IdempotencyRecordEntity;
import com.kori.adapters.out.jpa.entity.LedgerEntryEntity;
import com.kori.application.command.MerchantWithdrawAtAgentCommand;
import com.kori.application.port.in.MerchantWithdrawAtAgentUseCase;
import com.kori.application.result.MerchantWithdrawAtAgentResult;
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

class MerchantWithdrawAtAgentUseCaseIT extends AbstractIntegrationTest {

    @Autowired MerchantWithdrawAtAgentUseCase merchantWithdrawAtAgentUseCase;

    @Test
    void happyPath_agentInitiatesMerchantWithdraw_createsTx_ledger_audit_and_idempotency() {
        // Given (seed Flyway)
        String agentId = "AGENT_001";
        String merchantId = "MERCHANT_001";

        BigDecimal amount = new BigDecimal("1000.00");
        BigDecimal expectedFee = new BigDecimal("10.00");
        BigDecimal expectedCommission = new BigDecimal("5.00");
        BigDecimal expectedPlatformRevenue = new BigDecimal("5.00");
        BigDecimal expectedTotalDebitedMerchant = new BigDecimal("1010.00");

        // ✅ Capture balances BEFORE (global DB state might not be zero)
        BigDecimal agentBalanceBefore = ledgerEntryJpaRepository.netBalance("AGENT", agentId);
        BigDecimal merchantBalanceBefore = ledgerEntryJpaRepository.netBalance("MERCHANT", merchantId);

        long auditBefore = auditEventJpaRepository.count();
        String idempotencyKey = idemKey("it-mw");

        MerchantWithdrawAtAgentCommand cmd = new MerchantWithdrawAtAgentCommand(
                idempotencyKey,
                new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                merchantId,
                agentId,
                amount
        );

        // When
        MerchantWithdrawAtAgentResult result = merchantWithdrawAtAgentUseCase.execute(cmd);

        // Then (business result)
        assertNotNull(result);
        assertNotNull(result.transactionId());
        assertEquals(merchantId, result.merchantId());
        assertEquals(agentId, result.agentId());

        assertEquals(0, result.amount().compareTo(amount));
        assertEquals(0, result.fee().compareTo(expectedFee));
        assertEquals(0, result.commission().compareTo(expectedCommission));
        assertEquals(0, result.totalDebitedMerchant().compareTo(expectedTotalDebitedMerchant));

        UUID txId = UUID.fromString(result.transactionId());

        // Ledger entries (4 entries)
        List<LedgerEntryEntity> ledger = ledgerEntryJpaRepository.findByTransactionIdOrderByCreatedAtAscIdAsc(txId);
        assertEquals(4, ledger.size(), "MerchantWithdrawAtAgent should append 4 ledger entries");

        boolean hasMerchantDebit1010 = ledger.stream().anyMatch(e ->
                "MERCHANT".equals(e.getAccount())
                        && "DEBIT".equals(e.getEntryType())
                        && merchantId.equals(e.getReferenceId())
                        && e.getAmount().compareTo(expectedTotalDebitedMerchant) == 0
        );

        boolean hasPlatformClearingCredit1000 = ledger.stream().anyMatch(e ->
                "PLATFORM_CLEARING".equals(e.getAccount())
                        && "CREDIT".equals(e.getEntryType())
                        && e.getReferenceId() == null
                        && e.getAmount().compareTo(amount) == 0
        );

        boolean hasAgentCredit5 = ledger.stream().anyMatch(e ->
                "AGENT".equals(e.getAccount())
                        && "CREDIT".equals(e.getEntryType())
                        && agentId.equals(e.getReferenceId())
                        && e.getAmount().compareTo(expectedCommission) == 0
        );

        boolean hasPlatformCredit5 = ledger.stream().anyMatch(e ->
                "PLATFORM".equals(e.getAccount())
                        && "CREDIT".equals(e.getEntryType())
                        && e.getReferenceId() == null
                        && e.getAmount().compareTo(expectedPlatformRevenue) == 0
        );

        assertTrue(hasMerchantDebit1010, "Should debit MERCHANT by amount + fee");
        assertTrue(hasPlatformClearingCredit1000, "Should credit PLATFORM_CLEARING by amount");
        assertTrue(hasAgentCredit5, "Should credit AGENT by commission");
        assertTrue(hasPlatformCredit5, "Should credit PLATFORM by (fee - commission)");

        // ✅ Verify DELTA balances (after - before)
        BigDecimal agentBalanceAfter = ledgerEntryJpaRepository.netBalance("AGENT", agentId);
        BigDecimal merchantBalanceAfter = ledgerEntryJpaRepository.netBalance("MERCHANT", merchantId);

        assertEquals(0, agentBalanceAfter.subtract(agentBalanceBefore).compareTo(expectedCommission),
                "Agent balance should increase by commission for this transaction");

        assertEquals(0, merchantBalanceAfter.subtract(merchantBalanceBefore)
                        .compareTo(expectedTotalDebitedMerchant.negate()),
                "Merchant balance should decrease by (amount + fee) for this transaction");

        // Audit
        long auditAfter = auditEventJpaRepository.count();
        assertEquals(auditBefore + 1, auditAfter, "One audit event should be written");

        List<AuditEventEntity> audits = auditEventJpaRepository.findAll();
        assertTrue(audits.stream().anyMatch(a ->
                "MERCHANT_WITHDRAW_AT_AGENT".equals(a.getAction())
                        && "AGENT".equals(a.getActorType())
                        && "agent-actor-it".equals(a.getActorId())
                        && a.getMetadataJson() != null
                        && a.getMetadataJson().contains("\"merchantId\":\"" + merchantId + "\"")
                        && a.getMetadataJson().contains("\"agentId\":\"" + agentId + "\"")
                        && a.getMetadataJson().contains("\"transactionId\":\"" + result.transactionId() + "\"")
        ));

        // Idempotency
        Optional<IdempotencyRecordEntity> idemOpt = idempotencyJpaRepository.findById(idempotencyKey);
        assertTrue(idemOpt.isPresent());
        IdempotencyRecordEntity idem = idemOpt.get();
        assertEquals(MerchantWithdrawAtAgentResult.class.getName(), idem.getResultType());
        assertNotNull(idem.getResultJson());
        assertFalse(idem.getResultJson().isBlank());
    }
}
