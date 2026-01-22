package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.CardEntity;
import com.kori.application.command.EnrollCardCommand;
import com.kori.application.command.PayByCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.EnrollCardUseCase;
import com.kori.application.port.in.PayByCardUseCase;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PayByCardForbiddenWhenCardLostIT extends AbstractIntegrationTest {

    @Autowired EnrollCardUseCase enrollCardUseCase;
    @Autowired PayByCardUseCase payByCardUseCase;

    @Test
    void payByCard_isForbidden_whenCardStatusIsLost() {
        // Given
        String agentId = "AGENT_001";
        String terminalId = "TERMINAL_001";

        String phoneNumber = randomPhone269();
        String cardUid = randomCardUid();
        String pin = "1234";

        enrollCardUseCase.execute(new EnrollCardCommand(
                idemKey("it-enroll-for-lost-pay"),
                new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                phoneNumber,
                cardUid,
                pin,
                agentId
        ));

        // Force LOST status directly
        CardEntity card = cardJpaRepository.findByCardUid(cardUid)
                .orElseThrow(() -> new AssertionError("Card should exist"));
        card.setStatus("LOST");
        cardJpaRepository.saveAndFlush(card);

        long txBefore = transactionJpaRepository.count();
        long ledgerBefore = ledgerEntryJpaRepository.count();
        long auditBefore = auditEventJpaRepository.count();
        long idemBefore = idempotencyJpaRepository.count();

        // When / Then
        assertThrows(ForbiddenOperationException.class, () ->
                payByCardUseCase.execute(new PayByCardCommand(
                        idemKey("it-pay-lost"),
                        new ActorContext(ActorType.TERMINAL, "terminal-actor-it", Map.of()),
                        terminalId,
                        cardUid,
                        pin,
                        new BigDecimal("500.00")
                ))
        );

        // And: no side effects
        assertEquals(txBefore, transactionJpaRepository.count());
        assertEquals(ledgerBefore, ledgerEntryJpaRepository.count());
        assertEquals(auditBefore, auditEventJpaRepository.count());
        assertEquals(idemBefore, idempotencyJpaRepository.count());
    }
}
