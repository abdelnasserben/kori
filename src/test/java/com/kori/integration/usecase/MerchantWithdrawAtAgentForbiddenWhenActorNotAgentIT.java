package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.repo.AuditEventJpaRepository;
import com.kori.adapters.out.jpa.repo.IdempotencyJpaRepository;
import com.kori.adapters.out.jpa.repo.LedgerEntryJpaRepository;
import com.kori.adapters.out.jpa.repo.TransactionJpaRepository;
import com.kori.application.command.MerchantWithdrawAtAgentCommand;
import com.kori.application.exception.ForbiddenOperationException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class MerchantWithdrawAtAgentForbiddenWhenActorNotAgentIT {

    @Autowired MerchantWithdrawAtAgentUseCase merchantWithdrawAtAgentUseCase;

    @Autowired TransactionJpaRepository transactionJpaRepository;
    @Autowired LedgerEntryJpaRepository ledgerEntryJpaRepository;
    @Autowired AuditEventJpaRepository auditEventJpaRepository;
    @Autowired IdempotencyJpaRepository idempotencyJpaRepository;

    @Test
    void merchantWithdraw_isForbidden_whenActorIsNotAgent() {
        // Given
        String merchantId = "MERCHANT_001";
        String agentId = "AGENT_001";

        long txBefore = transactionJpaRepository.count();
        long ledgerBefore = ledgerEntryJpaRepository.count();
        long auditBefore = auditEventJpaRepository.count();
        long idemBefore = idempotencyJpaRepository.count();

        // When / Then (TERMINAL tries)
        assertThrows(ForbiddenOperationException.class, () ->
                merchantWithdrawAtAgentUseCase.execute(new MerchantWithdrawAtAgentCommand(
                        "it-mw-forbidden-" + UUID.randomUUID(),
                        new ActorContext(ActorType.TERMINAL, "terminal-actor-it", Map.of()),
                        merchantId,
                        agentId,
                        new BigDecimal("1000.00")
                ))
        );

        // And: no side effects
        assertEquals(txBefore, transactionJpaRepository.count());
        assertEquals(ledgerBefore, ledgerEntryJpaRepository.count());
        assertEquals(auditBefore, auditEventJpaRepository.count());
        assertEquals(idemBefore, idempotencyJpaRepository.count());
    }
}
