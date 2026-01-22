package com.kori.integration.usecase;

import com.kori.application.command.EnrollCardCommand;
import com.kori.application.command.PayByCardCommand;
import com.kori.application.command.ReversalCommand;
import com.kori.application.exception.ForbiddenOperationException;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReversalForbiddenWhenAlreadyReversedIT extends AbstractIntegrationTest {

    @Autowired EnrollCardUseCase enrollCardUseCase;
    @Autowired PayByCardUseCase payByCardUseCase;
    @Autowired ReversalUseCase reversalUseCase;

    @Test
    void reversal_isForbidden_whenOriginalTransactionAlreadyReversed() {
        // Given: original PAY_BY_CARD exists
        String agentId = "AGENT_001";
        String terminalId = "TERMINAL_001";

        String phoneNumber = randomPhone269();
        String cardUid = randomCardUid();
        String pin = "1234";

        enrollCardUseCase.execute(new EnrollCardCommand(
                idemKey("it-enroll-for-already-reversed"),
                new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                phoneNumber,
                cardUid,
                pin,
                agentId
        ));

        PayByCardResult pay = payByCardUseCase.execute(new PayByCardCommand(
                idemKey("it-pay-for-already-reversed"),
                new ActorContext(ActorType.TERMINAL, "terminal-actor-it", Map.of()),
                terminalId,
                cardUid,
                pin,
                new BigDecimal("1000.00")
        ));

        String originalTxId = pay.transactionId();
        assertNotNull(originalTxId);

        // First reversal -> OK
        ReversalResult first = reversalUseCase.execute(new ReversalCommand(
                idemKey("it-reversal-first"),
                new ActorContext(ActorType.ADMIN, "admin-actor-it", Map.of()),
                originalTxId
        ));
        assertNotNull(first);
        assertNotNull(first.reversalTransactionId());

        // Capture counts AFTER first reversal
        long txAfterFirst = transactionJpaRepository.count();
        long ledgerAfterFirst = ledgerEntryJpaRepository.count();
        long auditAfterFirst = auditEventJpaRepository.count();
        long idemAfterFirst = idempotencyJpaRepository.count();

        // Second reversal (different idempotency key) should be forbidden
        assertThrows(ForbiddenOperationException.class, () ->
                reversalUseCase.execute(new ReversalCommand(
                        idemKey("it-reversal-second"),
                        new ActorContext(ActorType.ADMIN, "admin-actor-it", Map.of()),
                        originalTxId
                ))
        );

        // And: no side effects for second attempt
        assertEquals(txAfterFirst, transactionJpaRepository.count(), "No extra transaction should be created");
        assertEquals(ledgerAfterFirst, ledgerEntryJpaRepository.count(), "No extra ledger entries should be created");
        assertEquals(auditAfterFirst, auditEventJpaRepository.count(), "No extra audit event should be created");
        assertEquals(idemAfterFirst, idempotencyJpaRepository.count(), "No idempotency record should be created");
    }
}
