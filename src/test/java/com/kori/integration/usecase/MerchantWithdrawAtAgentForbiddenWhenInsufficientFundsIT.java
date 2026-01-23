package com.kori.integration.usecase;

import com.kori.application.command.MerchantWithdrawAtAgentCommand;
import com.kori.application.exception.InsufficientFundsException;
import com.kori.application.port.in.MerchantWithdrawAtAgentUseCase;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MerchantWithdrawAtAgentForbiddenWhenInsufficientFundsIT extends AbstractIntegrationTest {

    @Autowired MerchantWithdrawAtAgentUseCase merchantWithdrawAtAgentUseCase;

    @Test
    void merchantWithdraw_isForbidden_whenInsufficientFunds() {
        // Given
        String agentId = "AGENT_001";
        String merchantId = "MERCHANT_001";

        long txBefore = transactionJpaRepository.count();
        long ledgerBefore = ledgerEntryJpaRepository.count();
        long auditBefore = auditEventJpaRepository.count();
        String idemKey = idemKey("it-mw-insufficient");

        // When / Then
        assertThrows(InsufficientFundsException.class, () -> merchantWithdrawAtAgentUseCase.execute(
                new MerchantWithdrawAtAgentCommand(
                        idemKey,
                        new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                        merchantId,
                        agentId,
                        new BigDecimal("1000.00") // total debited = 1010.00, balance may be 0
                )
        ));

        // And: no side effects
        assertEquals(txBefore, transactionJpaRepository.count());
        assertEquals(ledgerBefore, ledgerEntryJpaRepository.count());
        assertEquals(auditBefore, auditEventJpaRepository.count());
    }
}
