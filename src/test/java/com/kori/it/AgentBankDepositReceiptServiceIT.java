package com.kori.it;

import com.kori.application.command.AgentBankDepositReceiptCommand;
import com.kori.application.port.in.AgentBankDepositReceiptUseCase;
import com.kori.application.result.AgentBankDepositReceiptResult;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.TransactionId;
import com.kori.domain.model.transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentBankDepositReceiptServiceIT extends IntegrationTestBase {

    @Autowired
    AgentBankDepositReceiptUseCase agentBankDepositReceiptUseCase;

    @Test
    void recordsBankDepositReceipt_andReducesAgentCashExposure() {
        Agent agent = createActiveAgent("A-998877");

        LedgerAccountRef clearing = LedgerAccountRef.agentCashClearing(agent.id().value().toString());
        seedLedgerDebit(clearing, new BigDecimal("120.00"));

        Money before = ledgerQueryPort.netBalance(clearing);
        assertEquals(new BigDecimal("-120.00"), before.asBigDecimal());

        AgentBankDepositReceiptResult result = agentBankDepositReceiptUseCase.execute(
                new AgentBankDepositReceiptCommand(
                        "idem-bank-1",
                        "request-hash",
                        adminActor(),
                        "A-998877",
                        new BigDecimal("70.00")
                )
        );

        var entries = ledgerQueryPort.findByTransactionId(TransactionId.of(result.transactionId()));
        assertEquals(2, entries.size());
        assertTrue(entries.stream().anyMatch(e -> e.accountRef().equals(LedgerAccountRef.platformBank()) && e.type().name().equals("DEBIT")));
        assertTrue(entries.stream().anyMatch(e -> e.accountRef().equals(clearing) && e.type().name().equals("CREDIT")));

        Money after = ledgerQueryPort.netBalance(clearing);
        assertEquals(new BigDecimal("-50.00"), after.asBigDecimal());

        assertEquals(TransactionType.AGENT_BANK_DEPOSIT_RECEIPT,
                transactionRepositoryPort.findById(TransactionId.of(result.transactionId())).orElseThrow().type());

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().equals("AGENT_BANK_DEPOSIT_RECEIPT")));
    }
}
