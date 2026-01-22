package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.repo.AuditEventJpaRepository;
import com.kori.adapters.out.jpa.repo.IdempotencyJpaRepository;
import com.kori.adapters.out.jpa.repo.LedgerEntryJpaRepository;
import com.kori.adapters.out.jpa.repo.TransactionJpaRepository;
import com.kori.application.command.MerchantWithdrawAtAgentCommand;
import com.kori.application.port.in.MerchantWithdrawAtAgentUseCase;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MerchantWithdrawAtAgentInvalidAmountIT {

    @Autowired MerchantWithdrawAtAgentUseCase merchantWithdrawAtAgentUseCase;

    @Autowired TransactionJpaRepository transactionJpaRepository;
    @Autowired LedgerEntryJpaRepository ledgerEntryJpaRepository;
    @Autowired AuditEventJpaRepository auditEventJpaRepository;
    @Autowired IdempotencyJpaRepository idempotencyJpaRepository;

    @Test
    void merchantWithdraw_isRejected_whenAmountIsZeroOrNegative() {
        // Given
        String merchantId = "MERCHANT_001";
        String agentId = "AGENT_001";

        long txBefore = transactionJpaRepository.count();
        long ledgerBefore = ledgerEntryJpaRepository.count();
        long auditBefore = auditEventJpaRepository.count();
        long idemBefore = idempotencyJpaRepository.count();

        // When / Then (amount = 0)
        RuntimeException ex1 = assertThrows(RuntimeException.class, () ->
                merchantWithdrawAtAgentUseCase.execute(new MerchantWithdrawAtAgentCommand(
                        "it-mw-zero-" + UUID.randomUUID(),
                        new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                        merchantId,
                        agentId,
                        BigDecimal.ZERO
                ))
        );

        // When / Then (amount < 0)
        RuntimeException ex2 = assertThrows(RuntimeException.class, () ->
                merchantWithdrawAtAgentUseCase.execute(new MerchantWithdrawAtAgentCommand(
                        "it-mw-negative-" + UUID.randomUUID(),
                        new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                        merchantId,
                        agentId,
                        new BigDecimal("-100.00")
                ))
        );

        // Optional: minimal signal that it's about amount (only if message exists)
        // (We don't enforce exact message to keep test stable.)
        assertNotNull(ex1);
        assertNotNull(ex2);

        // And: no side effects
        assertEquals(txBefore, transactionJpaRepository.count(), "No transaction should be created");
        assertEquals(ledgerBefore, ledgerEntryJpaRepository.count(), "No ledger entries should be created");
        assertEquals(auditBefore, auditEventJpaRepository.count(), "No audit event should be created");
        assertEquals(idemBefore, idempotencyJpaRepository.count(), "No idempotency record should be created");
    }
}
