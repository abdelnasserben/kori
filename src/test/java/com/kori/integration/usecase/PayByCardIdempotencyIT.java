package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.IdempotencyRecordEntity;
import com.kori.adapters.out.jpa.entity.LedgerEntryEntity;
import com.kori.adapters.out.jpa.repo.IdempotencyJpaRepository;
import com.kori.adapters.out.jpa.repo.LedgerEntryJpaRepository;
import com.kori.adapters.out.jpa.repo.TransactionJpaRepository;
import com.kori.application.command.EnrollCardCommand;
import com.kori.application.command.PayByCardCommand;
import com.kori.application.port.in.EnrollCardUseCase;
import com.kori.application.port.in.PayByCardUseCase;
import com.kori.application.result.PayByCardResult;
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
class PayByCardIdempotencyIT {

    @Autowired EnrollCardUseCase enrollCardUseCase;
    @Autowired PayByCardUseCase payByCardUseCase;

    @Autowired TransactionJpaRepository transactionJpaRepository;
    @Autowired LedgerEntryJpaRepository ledgerEntryJpaRepository;
    @Autowired IdempotencyJpaRepository idempotencyJpaRepository;

    @Test
    void idempotency_sameKey_twice_returnsSameResult_andDoesNotDuplicateTxOrLedger() {
        // Given (seed Flyway)
        String agentId = "AGENT_001";
        String terminalId = "TERMINAL_001";

        // Setup a card via real use case
        String phoneNumber = "+269910" + (100000 + (int) (Math.random() * 899999));
        String cardUid = "CARD-" + UUID.randomUUID();
        String pin = "1234";

        enrollCardUseCase.execute(new EnrollCardCommand(
                "it-enroll-for-idem-pay-" + UUID.randomUUID(),
                new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                phoneNumber,
                cardUid,
                pin,
                agentId
        ));

        BigDecimal amount = new BigDecimal("1000.00"); // fee = 20.00 (2%)
        String sameIdempotencyKey = "it-pay-idem-" + UUID.randomUUID();

        PayByCardCommand cmd = new PayByCardCommand(
                sameIdempotencyKey,
                new ActorContext(ActorType.TERMINAL, "terminal-actor-it", Map.of()),
                terminalId,
                cardUid,
                pin,
                amount
        );

        // When (first call)
        PayByCardResult r1 = payByCardUseCase.execute(cmd);

        // Capture DB counts AFTER first call
        long txCountAfterFirst = transactionJpaRepository.count();
        long ledgerCountAfterFirst = ledgerEntryJpaRepository.count();

        // Ledger rows for the created tx should be 3
        UUID txId = UUID.fromString(r1.transactionId());
        List<LedgerEntryEntity> ledgerForTxAfterFirst =
                ledgerEntryJpaRepository.findByTransactionIdOrderByCreatedAtAscIdAsc(txId);
        assertEquals(3, ledgerForTxAfterFirst.size(), "PAY_BY_CARD should create 3 ledger entries");

        // When (second call with same idempotency key and exact same command)
        PayByCardResult r2 = payByCardUseCase.execute(cmd);

        // Then: same business result (at least transactionId must match)
        assertNotNull(r2);
        assertEquals(r1.transactionId(), r2.transactionId(), "Idempotent call must return same transactionId");
        assertEquals(r1.clientId(), r2.clientId(), "Idempotent call must return same clientId");
        assertEquals(r1.merchantId(), r2.merchantId(), "Idempotent call must return same merchantId");
        assertEquals(0, r1.amount().compareTo(r2.amount()));
        assertEquals(0, r1.fee().compareTo(r2.fee()));
        assertEquals(0, r1.totalDebited().compareTo(r2.totalDebited()));

        // Then: no DB duplication on second call
        assertEquals(txCountAfterFirst, transactionJpaRepository.count(),
                "Second idempotent call must NOT create a new transaction row");
        assertEquals(ledgerCountAfterFirst, ledgerEntryJpaRepository.count(),
                "Second idempotent call must NOT create new ledger rows");

        // Ledger rows for that tx remain exactly 3
        List<LedgerEntryEntity> ledgerForTxAfterSecond =
                ledgerEntryJpaRepository.findByTransactionIdOrderByCreatedAtAscIdAsc(txId);
        assertEquals(3, ledgerForTxAfterSecond.size(), "Ledger entries for tx must remain 3");

        // Idempotency record exists
        Optional<IdempotencyRecordEntity> idemOpt =
                idempotencyJpaRepository.findById(sameIdempotencyKey);
        assertTrue(idemOpt.isPresent(), "Idempotency record must exist");
        assertEquals(PayByCardResult.class.getName(), idemOpt.get().getResultType());
        assertNotNull(idemOpt.get().getResultJson());
        assertFalse(idemOpt.get().getResultJson().isBlank());
    }
}
