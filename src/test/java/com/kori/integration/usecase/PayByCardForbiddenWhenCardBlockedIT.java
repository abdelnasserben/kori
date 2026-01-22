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

class PayByCardForbiddenWhenCardBlockedIT extends AbstractIntegrationTest {

    @Autowired EnrollCardUseCase enrollCardUseCase;
    @Autowired PayByCardUseCase payByCardUseCase;

    @Test
    void payByCard_isForbidden_whenCardStatusIsBlocked() {
        // Given: create a card via EnrollCard
        String agentId = "AGENT_001";
        String terminalId = "TERMINAL_001";

        String phoneNumber = randomPhone269();
        String cardUid = randomCardUid();
        String pin = "1234";

        enrollCardUseCase.execute(new EnrollCardCommand(
                idemKey("it-enroll-for-blocked-pay"),
                new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                phoneNumber,
                cardUid,
                pin,
                agentId
        ));

        // Force BLOCKED status directly in DB (no other use case)
        CardEntity card = cardJpaRepository.findByCardUid(cardUid)
                .orElseThrow(() -> new AssertionError("Card should exist after enrollment"));

        card.setStatus("BLOCKED");
        cardJpaRepository.saveAndFlush(card);

        long txBefore = transactionJpaRepository.count();
        long ledgerBefore = ledgerEntryJpaRepository.count();
        long auditBefore = auditEventJpaRepository.count();
        long idemBefore = idempotencyJpaRepository.count();

        // When / Then
        assertThrows(ForbiddenOperationException.class, () ->
                payByCardUseCase.execute(new PayByCardCommand(
                        idemKey("it-pay-blocked"),
                        new ActorContext(ActorType.TERMINAL, "terminal-actor-it", Map.of()),
                        terminalId,
                        cardUid,
                        pin,
                        new BigDecimal("1000.00")
                ))
        );

        // And: nothing persisted
        assertEquals(txBefore, transactionJpaRepository.count(), "No transaction should be created");
        assertEquals(ledgerBefore, ledgerEntryJpaRepository.count(), "No ledger entry should be created");
        assertEquals(auditBefore, auditEventJpaRepository.count(), "No audit event should be created");
        assertEquals(idemBefore, idempotencyJpaRepository.count(), "No idempotency record should be created");
    }
}
