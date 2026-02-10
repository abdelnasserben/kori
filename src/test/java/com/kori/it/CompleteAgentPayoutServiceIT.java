package com.kori.it;

import com.kori.application.command.CompleteAgentPayoutCommand;
import com.kori.application.port.in.CompleteAgentPayoutUseCase;
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

class CompleteAgentPayoutServiceIT extends IntegrationTestBase {

    private static final String AGENT_CODE = "A-777888";

    @Autowired
    CompleteAgentPayoutUseCase completeAgentPayoutUseCase;

    @Test
    void completeAgentPayout_happyPath_writesLedgerAndAudit() {
        Agent agent = createActiveAgent(AGENT_CODE);

        TransactionId txId = new TransactionId(UUID.randomUUID());
        Transaction tx = new Transaction(txId, TransactionType.AGENT_PAYOUT, Money.of(new BigDecimal("40.00")), NOW, null);
        transactionRepositoryPort.save(tx);

        Payout payout = Payout.requested(new PayoutId(UUID.randomUUID()), agent.id(), tx.id(), Money.of(new BigDecimal("40.00")), NOW);
        payoutRepositoryPort.save(payout);

        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(tx.id(), LedgerAccountRef.agentWallet(agent.id().value().toString()), Money.of(new BigDecimal("40.00"))),
                LedgerEntry.credit(tx.id(), LedgerAccountRef.platformClearing(), Money.of(new BigDecimal("40.00")))
        ));

        completeAgentPayoutUseCase.execute(new CompleteAgentPayoutCommand(
                adminActor(),
                payout.id().value().toString()
        ));

        Payout updated = payoutRepositoryPort.findById(payout.id()).orElseThrow();
        assertEquals(PayoutStatus.COMPLETED, updated.status());
        assertNotNull(updated.completedAt());

        List<LedgerEntry> entries = ledgerQueryPort.findByTransactionId(tx.id());
        assertEquals(4, entries.size());

        assertTrue(entries.stream().anyMatch(entry ->
                entry.type() == LedgerEntryType.DEBIT
                        && entry.accountRef().equals(LedgerAccountRef.platformClearing())
                        && entry.amount().equals(Money.of(new BigDecimal("40.00")))
        ));

        assertTrue(entries.stream().anyMatch(entry ->
                entry.type() == LedgerEntryType.CREDIT
                        && entry.accountRef().equals(LedgerAccountRef.platformBank())
                        && entry.amount().equals(Money.of(new BigDecimal("40.00")))
        ));

        assertEquals(Money.zero(), ledgerQueryPort.netBalance(LedgerAccountRef.platformClearing()));
        assertEquals(Money.of(new BigDecimal("40.00")), ledgerQueryPort.netBalance(LedgerAccountRef.platformBank()));

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().equals("AGENT_PAYOUT_COMPLETED"))
        );
    }

    @Test
    void completeAgentPayout_isIdempotent_whenAlreadyCompleted() {
        Agent agent = createActiveAgent("A-777889");

        TransactionId txId = new TransactionId(UUID.randomUUID());
        Transaction tx = new Transaction(txId, TransactionType.AGENT_PAYOUT, Money.of(new BigDecimal("40.00")), NOW, null);
        transactionRepositoryPort.save(tx);

        Payout payout = Payout.requested(new PayoutId(UUID.randomUUID()), agent.id(), tx.id(), Money.of(new BigDecimal("40.00")), NOW);
        payoutRepositoryPort.save(payout);

        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(tx.id(), LedgerAccountRef.agentWallet(agent.id().value().toString()), Money.of(new BigDecimal("40.00"))),
                LedgerEntry.credit(tx.id(), LedgerAccountRef.platformClearing(), Money.of(new BigDecimal("40.00")))
        ));

        completeAgentPayoutUseCase.execute(new CompleteAgentPayoutCommand(adminActor(), payout.id().value().toString()));
        int ledgerCountAfterFirstCall = ledgerQueryPort.findByTransactionId(tx.id()).size();
        long completedAuditAfterFirstCall = auditEventJpaRepository.findAll().stream()
                .filter(event -> event.getAction().equals("AGENT_PAYOUT_COMPLETED"))
                .count();

        completeAgentPayoutUseCase.execute(new CompleteAgentPayoutCommand(adminActor(), payout.id().value().toString()));

        assertEquals(ledgerCountAfterFirstCall, ledgerQueryPort.findByTransactionId(tx.id()).size());
        assertEquals(completedAuditAfterFirstCall, auditEventJpaRepository.findAll().stream()
                .filter(event -> event.getAction().equals("AGENT_PAYOUT_COMPLETED"))
                .count());

        Payout updated = payoutRepositoryPort.findById(payout.id()).orElseThrow();
        assertEquals(PayoutStatus.COMPLETED, updated.status());
    }
}
