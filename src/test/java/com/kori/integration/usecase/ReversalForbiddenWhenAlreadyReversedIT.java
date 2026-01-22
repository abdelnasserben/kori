package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.repo.AuditEventJpaRepository;
import com.kori.adapters.out.jpa.repo.IdempotencyJpaRepository;
import com.kori.adapters.out.jpa.repo.LedgerEntryJpaRepository;
import com.kori.adapters.out.jpa.repo.TransactionJpaRepository;
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
class ReversalForbiddenWhenAlreadyReversedIT {

    @Autowired EnrollCardUseCase enrollCardUseCase;
    @Autowired PayByCardUseCase payByCardUseCase;
    @Autowired ReversalUseCase reversalUseCase;

    @Autowired TransactionJpaRepository transactionJpaRepository;
    @Autowired LedgerEntryJpaRepository ledgerEntryJpaRepository;
    @Autowired AuditEventJpaRepository auditEventJpaRepository;
    @Autowired IdempotencyJpaRepository idempotencyJpaRepository;

    @Test
    void reversal_isForbidden_whenOriginalTransactionAlreadyReversed() {
        // Given: original PAY_BY_CARD exists
        String agentId = "AGENT_001";
        String terminalId = "TERMINAL_001";

        String phoneNumber = "+269860" + (100000 + (int) (Math.random() * 899999));
        String cardUid = "CARD-" + UUID.randomUUID();
        String pin = "1234";

        enrollCardUseCase.execute(new EnrollCardCommand(
                "it-enroll-for-already-reversed-" + UUID.randomUUID(),
                new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                phoneNumber,
                cardUid,
                pin,
                agentId
        ));

        PayByCardResult pay = payByCardUseCase.execute(new PayByCardCommand(
                "it-pay-for-already-reversed-" + UUID.randomUUID(),
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
                "it-reversal-first-" + UUID.randomUUID(),
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
                        "it-reversal-second-" + UUID.randomUUID(),
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
