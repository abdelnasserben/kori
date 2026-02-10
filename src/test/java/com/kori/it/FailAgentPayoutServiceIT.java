package com.kori.it;

import com.kori.application.command.FailAgentPayoutCommand;
import com.kori.application.port.in.FailAgentPayoutUseCase;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.ledger.LedgerEntryType;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.payout.Payout;
import com.kori.domain.model.payout.PayoutId;
import com.kori.domain.model.payout.PayoutStatus;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;
import com.kori.domain.model.transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FailAgentPayoutServiceIT extends IntegrationTestBase {

    private static final String AGENT_CODE = "A-808080";

    @Autowired
    FailAgentPayoutUseCase failAgentPayoutUseCase;

    @Test
    void failAgentPayout_marksFailedAndWritesAudit() {
        Agent agent = createActiveAgent(AGENT_CODE);

        TransactionId txId = new TransactionId(UUID.randomUUID());
        Transaction tx = new Transaction(txId, TransactionType.AGENT_PAYOUT, Money.of(new BigDecimal("60.00")), NOW, null);
        transactionRepositoryPort.save(tx);

        Payout payout = Payout.requested(new PayoutId(UUID.randomUUID()), agent.id(), tx.id(), Money.of(new BigDecimal("60.00")), NOW);
        payoutRepositoryPort.save(payout);

        LedgerAccountRef walletAcc = LedgerAccountRef.agentWallet(agent.id().value().toString());
        seedLedgerCredit(walletAcc, new BigDecimal("60.00"));

        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(tx.id(), LedgerAccountRef.agentWallet(agent.id().value().toString()), Money.of(new BigDecimal("60.00"))),
                LedgerEntry.credit(tx.id(), LedgerAccountRef.platformClearing(), Money.of(new BigDecimal("60.00")))
        ));

        failAgentPayoutUseCase.execute(new FailAgentPayoutCommand(
                adminActor(),
                payout.id().value().toString(),
                "bank error"
        ));

        Payout updated = payoutRepositoryPort.findById(payout.id()).orElseThrow();
        assertEquals(PayoutStatus.FAILED, updated.status());
        assertNotNull(updated.failedAt());
        assertEquals("bank error", updated.failureReason());

        var entries = ledgerQueryPort.findByTransactionId(tx.id());
        assertTrue(entries.stream().anyMatch(entry ->
                entry.type() == LedgerEntryType.DEBIT
                        && entry.accountRef().equals(LedgerAccountRef.platformClearing())
                        && entry.amount().equals(Money.of(new BigDecimal("60.00")))
        ));
        assertTrue(entries.stream().anyMatch(entry ->
                entry.type() == LedgerEntryType.CREDIT
                        && entry.accountRef().equals(LedgerAccountRef.agentWallet(agent.id().value().toString()))
                        && entry.amount().equals(Money.of(new BigDecimal("60.00")))
        ));

        assertEquals(Money.of(new BigDecimal("60.00")), ledgerQueryPort.netBalance(LedgerAccountRef.agentWallet(agent.id().value().toString())));
        assertEquals(Money.zero(), ledgerQueryPort.netBalance(LedgerAccountRef.platformClearing()));

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().equals("AGENT_PAYOUT_FAILED"))
        );
    }

    @Test
    void failAgentPayout_isIdempotent_whenAlreadyFailed() {
        Agent agent = createActiveAgent("A-808081");

        TransactionId txId = new TransactionId(UUID.randomUUID());
        Transaction tx = new Transaction(txId, TransactionType.AGENT_PAYOUT, Money.of(new BigDecimal("60.00")), NOW, null);
        transactionRepositoryPort.save(tx);

        Payout payout = Payout.requested(new PayoutId(UUID.randomUUID()), agent.id(), tx.id(), Money.of(new BigDecimal("60.00")), NOW);
        payoutRepositoryPort.save(payout);

        LedgerAccountRef walletAcc = LedgerAccountRef.agentWallet(agent.id().value().toString());
        seedLedgerCredit(walletAcc, new BigDecimal("60.00"));

        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(tx.id(), LedgerAccountRef.agentWallet(agent.id().value().toString()), Money.of(new BigDecimal("60.00"))),
                LedgerEntry.credit(tx.id(), LedgerAccountRef.platformClearing(), Money.of(new BigDecimal("60.00")))
        ));

        failAgentPayoutUseCase.execute(new FailAgentPayoutCommand(adminActor(), payout.id().value().toString(), "bank error"));
        int ledgerCountAfterFirstCall = ledgerQueryPort.findByTransactionId(tx.id()).size();
        long failedAuditAfterFirstCall = auditEventJpaRepository.findAll().stream()
                .filter(event -> event.getAction().equals("AGENT_PAYOUT_FAILED"))
                .count();

        failAgentPayoutUseCase.execute(new FailAgentPayoutCommand(adminActor(), payout.id().value().toString(), "bank error"));

        assertEquals(ledgerCountAfterFirstCall, ledgerQueryPort.findByTransactionId(tx.id()).size());
        assertEquals(failedAuditAfterFirstCall, auditEventJpaRepository.findAll().stream()
                .filter(event -> event.getAction().equals("AGENT_PAYOUT_FAILED"))
                .count());

        Payout updated = payoutRepositoryPort.findById(payout.id()).orElseThrow();
        assertEquals(PayoutStatus.FAILED, updated.status());
    }
}
