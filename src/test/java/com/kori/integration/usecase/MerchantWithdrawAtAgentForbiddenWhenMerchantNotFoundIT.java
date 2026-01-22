package com.kori.integration.usecase;

import com.kori.application.command.MerchantWithdrawAtAgentCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.MerchantWithdrawAtAgentUseCase;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MerchantWithdrawAtAgentForbiddenWhenMerchantNotFoundIT extends AbstractIntegrationTest {

    @Autowired MerchantWithdrawAtAgentUseCase merchantWithdrawAtAgentUseCase;

    @Test
    void merchantWithdraw_isForbidden_whenMerchantDoesNotExist() {
        // Given
        String agentId = "AGENT_001"; // seed Flyway => exists
        String merchantId = "MERCHANT_DOES_NOT_EXIST_" + UUID.randomUUID();

        long txBefore = transactionJpaRepository.count();
        long ledgerBefore = ledgerEntryJpaRepository.count();
        long auditBefore = auditEventJpaRepository.count();
        long idemBefore = idempotencyJpaRepository.count();

        // When / Then
        assertThrows(ForbiddenOperationException.class, () ->
                merchantWithdrawAtAgentUseCase.execute(new MerchantWithdrawAtAgentCommand(
                        idemKey("it-mw-merchant-not-found"),
                        new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                        merchantId,
                        agentId,
                        new BigDecimal("1000.00")
                ))
        );

        // And: no side effects
        assertEquals(txBefore, transactionJpaRepository.count(), "No transaction should be created");
        assertEquals(ledgerBefore, ledgerEntryJpaRepository.count(), "No ledger entries should be created");
        assertEquals(auditBefore, auditEventJpaRepository.count(), "No audit event should be created");
        assertEquals(idemBefore, idempotencyJpaRepository.count(), "No idempotency record should be created");
    }
}
