package com.kori.it;

import com.kori.application.command.RequestAgentPayoutCommand;
import com.kori.application.port.in.RequestAgentPayoutUseCase;
import com.kori.application.result.AgentPayoutResult;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.payout.Payout;
import com.kori.domain.model.payout.PayoutId;
import com.kori.domain.model.payout.PayoutStatus;
import com.kori.domain.model.transaction.TransactionId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RequestAgentPayoutServiceIT extends IntegrationTestBase {

    private static final String AGENT_CODE = "A-555666";

    @Autowired RequestAgentPayoutUseCase requestAgentPayoutUseCase;

    @Test
    void requestAgentPayout_happyPath_persistsPayoutAndAudit() {
        Agent agent = createActiveAgent(AGENT_CODE);

        LedgerAccountRef agentWalletAccount = LedgerAccountRef.agentWallet(agent.id().value().toString());
        seedLedgerCredit(agentWalletAccount, new BigDecimal("75.00"));

        AgentPayoutResult result = requestAgentPayoutUseCase.execute(new RequestAgentPayoutCommand(
                "idem-payout-1",
                "request-hash",
                adminActor(),
                AGENT_CODE
        ));

        assertNotNull(result.transactionId());
        assertNotNull(result.payoutId());
        assertEquals(PayoutStatus.REQUESTED.name(), result.payoutStatus());

        Payout payout = payoutRepositoryPort.findById(PayoutId.of(result.payoutId())).orElseThrow();
        assertEquals(PayoutStatus.REQUESTED, payout.status());

        var entries = ledgerQueryPort.findByTransactionId(TransactionId.of(result.transactionId()));
        assertEquals(2, entries.size());
        assertTrue(entries.stream().anyMatch(e -> e.accountRef().equals(agentWalletAccount) && e.type().name().equals("DEBIT")));
        assertTrue(entries.stream().anyMatch(e -> e.accountRef().equals(LedgerAccountRef.platformClearing()) && e.type().name().equals("CREDIT")));
        assertEquals(Money.zero(), ledgerQueryPort.netBalance(agentWalletAccount));

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().equals("AGENT_PAYOUT_REQUESTED"))
        );
    }
}
