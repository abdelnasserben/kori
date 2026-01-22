package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.IdempotencyRecordEntity;
import com.kori.adapters.out.jpa.entity.LedgerEntryEntity;
import com.kori.adapters.out.jpa.repo.IdempotencyJpaRepository;
import com.kori.adapters.out.jpa.repo.LedgerEntryJpaRepository;
import com.kori.adapters.out.jpa.repo.TransactionJpaRepository;
import com.kori.application.command.MerchantWithdrawAtAgentCommand;
import com.kori.application.port.in.MerchantWithdrawAtAgentUseCase;
import com.kori.application.result.MerchantWithdrawAtAgentResult;
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
class MerchantWithdrawAtAgentIdempotencyIT {

    @Autowired MerchantWithdrawAtAgentUseCase merchantWithdrawAtAgentUseCase;

    @Autowired TransactionJpaRepository transactionJpaRepository;
    @Autowired LedgerEntryJpaRepository ledgerEntryJpaRepository;
    @Autowired IdempotencyJpaRepository idempotencyJpaRepository;

    @Test
    void idempotency_sameKey_twice_returnsSameResult_andDoesNotDuplicateTxOrLedger() {
        // Given (seed Flyway)
        String agentId = "AGENT_001";
        String merchantId = "MERCHANT_001";

        BigDecimal amount = new BigDecimal("1000.00");
        String sameIdempotencyKey = "it-mw-idem-" + UUID.randomUUID();

        MerchantWithdrawAtAgentCommand cmd = new MerchantWithdrawAtAgentCommand(
                sameIdempotencyKey,
                new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                merchantId,
                agentId,
                amount
        );

        // When (first call)
        MerchantWithdrawAtAgentResult r1 = merchantWithdrawAtAgentUseCase.execute(cmd);
        assertNotNull(r1);
        assertNotNull(r1.transactionId());

        // Capture DB counts AFTER first call
        long txCountAfterFirst = transactionJpaRepository.count();
        long ledgerCountAfterFirst = ledgerEntryJpaRepository.count();

        // Ledger rows for the created tx should be 4
        UUID txId = UUID.fromString(r1.transactionId());
        List<LedgerEntryEntity> ledgerForTxAfterFirst =
                ledgerEntryJpaRepository.findByTransactionIdOrderByCreatedAtAscIdAsc(txId);
        assertEquals(4, ledgerForTxAfterFirst.size(), "MERCHANT_WITHDRAW_AT_AGENT should create 4 ledger entries");

        // When (second call with same idempotency key and exact same command)
        MerchantWithdrawAtAgentResult r2 = merchantWithdrawAtAgentUseCase.execute(cmd);

        // Then: same business result
        assertNotNull(r2);
        assertEquals(r1.transactionId(), r2.transactionId(), "Idempotent call must return same transactionId");
        assertEquals(r1.merchantId(), r2.merchantId(), "Idempotent call must return same merchantId");
        assertEquals(r1.agentId(), r2.agentId(), "Idempotent call must return same agentId");
        assertEquals(0, r1.amount().compareTo(r2.amount()));
        assertEquals(0, r1.fee().compareTo(r2.fee()));
        assertEquals(0, r1.commission().compareTo(r2.commission()));
        assertEquals(0, r1.totalDebitedMerchant().compareTo(r2.totalDebitedMerchant()));

        // Then: no DB duplication on second call
        assertEquals(txCountAfterFirst, transactionJpaRepository.count(),
                "Second idempotent call must NOT create a new transaction row");
        assertEquals(ledgerCountAfterFirst, ledgerEntryJpaRepository.count(),
                "Second idempotent call must NOT create new ledger rows");

        // Ledger rows for that tx remain exactly 4
        List<LedgerEntryEntity> ledgerForTxAfterSecond =
                ledgerEntryJpaRepository.findByTransactionIdOrderByCreatedAtAscIdAsc(txId);
        assertEquals(4, ledgerForTxAfterSecond.size(), "Ledger entries for tx must remain 4");

        // Idempotency record exists
        Optional<IdempotencyRecordEntity> idemOpt =
                idempotencyJpaRepository.findById(sameIdempotencyKey);
        assertTrue(idemOpt.isPresent(), "Idempotency record must exist");
        assertEquals(MerchantWithdrawAtAgentResult.class.getName(), idemOpt.get().getResultType());
        assertNotNull(idemOpt.get().getResultJson());
        assertFalse(idemOpt.get().getResultJson().isBlank());
    }
}
