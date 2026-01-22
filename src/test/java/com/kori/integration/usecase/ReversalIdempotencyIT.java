package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.IdempotencyRecordEntity;
import com.kori.adapters.out.jpa.entity.LedgerEntryEntity;
import com.kori.adapters.out.jpa.repo.IdempotencyJpaRepository;
import com.kori.adapters.out.jpa.repo.LedgerEntryJpaRepository;
import com.kori.adapters.out.jpa.repo.TransactionJpaRepository;
import com.kori.application.command.EnrollCardCommand;
import com.kori.application.command.PayByCardCommand;
import com.kori.application.command.ReversalCommand;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional // rollback automatique après CHAQUE test
class ReversalIdempotencyIT {

    @Autowired EnrollCardUseCase enrollCardUseCase;
    @Autowired PayByCardUseCase payByCardUseCase;
    @Autowired ReversalUseCase reversalUseCase;

    @Autowired TransactionJpaRepository transactionJpaRepository;
    @Autowired LedgerEntryJpaRepository ledgerEntryJpaRepository;
    @Autowired IdempotencyJpaRepository idempotencyJpaRepository;

    @Test
    void idempotency_sameKey_twice_returnsSameReversal_andDoesNotDuplicateTxOrLedger() {
        // Given: original PAY_BY_CARD exists
        String agentId = "AGENT_001";
        String terminalId = "TERMINAL_001";

        String phoneNumber = "+269820" + (100000 + (int) (Math.random() * 899999));
        String cardUid = "CARD-" + UUID.randomUUID();
        String pin = "1234";

        enrollCardUseCase.execute(new EnrollCardCommand(
                "it-enroll-for-reversal-idem-" + UUID.randomUUID(),
                new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                phoneNumber,
                cardUid,
                pin,
                agentId
        ));

        PayByCardResult pay = payByCardUseCase.execute(new PayByCardCommand(
                "it-pay-for-reversal-idem-" + UUID.randomUUID(),
                new ActorContext(ActorType.TERMINAL, "terminal-actor-it", Map.of()),
                terminalId,
                cardUid,
                pin,
                new BigDecimal("1000.00")
        ));

        String originalTxId = pay.transactionId();
        assertNotNull(originalTxId);

        String sameIdempotencyKey = "it-reversal-idem-" + UUID.randomUUID();

        ReversalCommand cmd = new ReversalCommand(
                sameIdempotencyKey,
                new ActorContext(ActorType.ADMIN, "admin-actor-it", Map.of()),
                originalTxId
        );

        // When (first reversal)
        ReversalResult r1 = reversalUseCase.execute(cmd);
        assertNotNull(r1);
        assertNotNull(r1.reversalTransactionId());
        assertEquals(originalTxId, r1.originalTransactionId());

        // Capture DB counts AFTER first call
        long txCountAfterFirst = transactionJpaRepository.count();
        long ledgerCountAfterFirst = ledgerEntryJpaRepository.count();

        UUID reversalTxId = UUID.fromString(r1.reversalTransactionId());
        List<LedgerEntryEntity> ledgerForReversalAfterFirst =
                ledgerEntryJpaRepository.findByTransactionIdOrderByCreatedAtAscIdAsc(reversalTxId);
        assertEquals(3, ledgerForReversalAfterFirst.size(), "REVERSAL should create 3 ledger entries");

        // When (second reversal with same idempotency key)
        ReversalResult r2 = reversalUseCase.execute(cmd);

        // Then: same business result
        assertNotNull(r2);
        assertEquals(r1.reversalTransactionId(), r2.reversalTransactionId(),
                "Idempotent call must return same reversalTransactionId");
        assertEquals(r1.originalTransactionId(), r2.originalTransactionId());

        // Then: no DB duplication on second call
        assertEquals(txCountAfterFirst, transactionJpaRepository.count(),
                "Second idempotent call must NOT create a new transaction row");
        assertEquals(ledgerCountAfterFirst, ledgerEntryJpaRepository.count(),
                "Second idempotent call must NOT create new ledger rows");

        // Ledger rows for reversal remain exactly 3
        List<LedgerEntryEntity> ledgerForReversalAfterSecond =
                ledgerEntryJpaRepository.findByTransactionIdOrderByCreatedAtAscIdAsc(reversalTxId);
        assertEquals(3, ledgerForReversalAfterSecond.size(), "Ledger entries for reversal must remain 3");

        // Idempotency record exists
        Optional<IdempotencyRecordEntity> idemOpt =
                idempotencyJpaRepository.findById(sameIdempotencyKey);
        assertTrue(idemOpt.isPresent(), "Idempotency record must exist");
        assertEquals(ReversalResult.class.getName(), idemOpt.get().getResultType());
        assertNotNull(idemOpt.get().getResultJson());
        assertFalse(idemOpt.get().getResultJson().isBlank());
    }
}
