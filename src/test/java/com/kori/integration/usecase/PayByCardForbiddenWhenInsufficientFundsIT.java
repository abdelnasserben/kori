package com.kori.integration.usecase;

import com.kori.application.command.EnrollCardCommand;
import com.kori.application.command.PayByCardCommand;
import com.kori.application.exception.InsufficientFundsException;
import com.kori.application.port.in.EnrollCardUseCase;
import com.kori.application.port.in.PayByCardUseCase;
import com.kori.application.result.EnrollCardResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PayByCardForbiddenWhenInsufficientFundsIT extends AbstractIntegrationTest {

    @Autowired EnrollCardUseCase enrollCardUseCase;
    @Autowired PayByCardUseCase payByCardUseCase;

    @Test
    void payByCard_isForbidden_whenInsufficientFunds() {
        // Given
        String agentId = "AGENT_001";
        String terminalId = "TERMINAL_001";

        String phoneNumber = randomPhone269();
        String cardUid = randomCardUid();
        String pin = "1234";

        EnrollCardResult enrolled = enrollCardUseCase.execute(
                new EnrollCardCommand(
                        idemKey("it-enroll-for-insufficient"),
                        new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                        phoneNumber,
                        cardUid,
                        pin,
                        agentId
                )
        );

        long txBefore = transactionJpaRepository.count();
        long ledgerBefore = ledgerEntryJpaRepository.count();
        long auditBefore = auditEventJpaRepository.count();
        String idemKey = idemKey("it-pay-insufficient");

        // When / Then
        assertThrows(InsufficientFundsException.class, () -> payByCardUseCase.execute(
                new PayByCardCommand(
                        idemKey,
                        new ActorContext(ActorType.TERMINAL, "terminal-actor-it", Map.of()),
                        terminalId,
                        cardUid,
                        pin,
                        new BigDecimal("1000.00") // total debited = 1020.00, balance = 0
                )
        ));

        // And: no side effects
        assertEquals(txBefore, transactionJpaRepository.count());
        assertEquals(ledgerBefore, ledgerEntryJpaRepository.count());
        assertEquals(auditBefore, auditEventJpaRepository.count());
    }
}
