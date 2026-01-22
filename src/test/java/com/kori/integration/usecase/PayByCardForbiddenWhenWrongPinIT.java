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

class PayByCardForbiddenWhenWrongPinIT extends AbstractIntegrationTest {

    @Autowired EnrollCardUseCase enrollCardUseCase;
    @Autowired PayByCardUseCase payByCardUseCase;

    @Test
    void payByCard_isForbidden_whenPinIsWrong() {
        // Given: active card
        String agentId = "AGENT_001";
        String terminalId = "TERMINAL_001";

        String phoneNumber = randomPhone269();
        String cardUid = randomCardUid();
        String correctPin = "1234";

        enrollCardUseCase.execute(new EnrollCardCommand(
                idemKey("it-enroll-for-wrong-pin"),
                new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                phoneNumber,
                cardUid,
                correctPin,
                agentId
        ));

        CardEntity cardBefore = cardJpaRepository.findByCardUid(cardUid)
                .orElseThrow(() -> new AssertionError("Card should exist"));

        int failedAttemptsBefore = cardBefore.getFailedPinAttempts();

        long txBefore = transactionJpaRepository.count();
        long ledgerBefore = ledgerEntryJpaRepository.count();
        long auditBefore = auditEventJpaRepository.count();
        long idemBefore = idempotencyJpaRepository.count();

        // When / Then
        assertThrows(ForbiddenOperationException.class, () ->
                payByCardUseCase.execute(new PayByCardCommand(
                        idemKey("it-pay-wrong-pin"),
                        new ActorContext(ActorType.TERMINAL, "terminal-actor-it", Map.of()),
                        terminalId,
                        cardUid,
                        "9999", // WRONG PIN
                        new BigDecimal("500.00")
                ))
        );

        // Reload card
        CardEntity cardAfter = cardJpaRepository.findByCardUid(cardUid).orElseThrow();

        // Then: failed pin attempts incremented
        assertEquals(failedAttemptsBefore + 1, cardAfter.getFailedPinAttempts(),
                "Failed PIN attempts should be incremented");

        // And: no side effects (except failedAttempts)
        assertEquals(txBefore, transactionJpaRepository.count(), "No transaction should be created");
        assertEquals(ledgerBefore, ledgerEntryJpaRepository.count(), "No ledger entry should be created");
        assertEquals(auditBefore, auditEventJpaRepository.count(), "No audit event should be created");
        assertEquals(idemBefore, idempotencyJpaRepository.count(), "No idempotency record should be created");
    }
}
