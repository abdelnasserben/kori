package com.kori.it;

import com.kori.application.command.CashInByAgentCommand;
import com.kori.application.port.in.CashInByAgentUseCase;
import com.kori.application.result.CashInByAgentResult;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.ledger.LedgerEntryType;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;
import com.kori.domain.model.transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CashInByAgentServiceIT extends IntegrationTestBase {

    @Autowired
    private CashInByAgentUseCase cashInByAgentUseCase;

    @Test
    void should_cash_in_by_agent_and_persist_side_effects() {
        Agent agent = createActiveAgent("A-123456");
        Client client = createActiveClient("+2690000000");

        BigDecimal amountBd = new BigDecimal("100.00");
        Money amount = Money.of(amountBd);

        CashInByAgentResult result = cashInByAgentUseCase.execute(
                new CashInByAgentCommand(
                        "idem-1",
                        "hash-1",
                        agentActor(agent.id().value().toString()),
                        client.phoneNumber(),
                        amountBd
                )
        );

        Transaction tx = transactionRepositoryPort.findById(TransactionId.of(result.transactionId())).orElseThrow();
        assertEquals(TransactionType.CASH_IN_BY_AGENT, tx.type());

        List<LedgerEntry> entries = ledgerQueryPort.findByTransactionId(TransactionId.of(result.transactionId()));
        assertEquals(2, entries.size());

        assertTrue(entries.stream().anyMatch(e ->
                e.accountRef().equals(LedgerAccountRef.platformClearing())
                        && e.type() == LedgerEntryType.DEBIT
                        && e.amount().equals(amount)
        ));

        assertTrue(entries.stream().anyMatch(e ->
                e.accountRef().equals(LedgerAccountRef.client(client.id().value().toString()))
                        && e.type() == LedgerEntryType.CREDIT
                        && e.amount().equals(amount)
        ));

        // Balances: agent inchangé, client +amount, clearing -amount (si netBalance suit DEBIT=-, CREDIT=+)
        Money clientBalance = ledgerQueryPort.netBalance(LedgerAccountRef.client(client.id().value().toString()));
        assertEquals(amount, clientBalance);

        Money clearingBalance = ledgerQueryPort.netBalance(LedgerAccountRef.platformClearing());
        assertEquals(Money.of(amountBd.negate()), clearingBalance);

        // Optionnel: agentBalance doit rester à 0 (ou à sa valeur seed si tu le seedais)
        Money agentBalance = ledgerQueryPort.netBalance(LedgerAccountRef.agent(agent.id().value().toString()));
        assertEquals(Money.of(BigDecimal.ZERO), agentBalance);

        var audits = auditEventJpaRepository.findAll();
        assertEquals(1, audits.size());
        assertEquals("AGENT_CASH_IN", audits.get(0).getAction());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM idempotency_records WHERE idempotency_key = ?",
                Integer.class,
                "idem-1"
        );
        assertEquals(1, count);
    }
}
