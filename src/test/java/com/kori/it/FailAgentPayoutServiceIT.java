package com.kori.it;

import com.kori.application.command.FailAgentPayoutCommand;
import com.kori.application.port.in.FailAgentPayoutUseCase;
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

        failAgentPayoutUseCase.execute(new FailAgentPayoutCommand(
                adminActor(),
                payout.id().value().toString(),
                "bank error"
        ));

        Payout updated = payoutRepositoryPort.findById(payout.id()).orElseThrow();
        assertEquals(PayoutStatus.FAILED, updated.status());
        assertNotNull(updated.failedAt());
        assertEquals("bank error", updated.failureReason());

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().equals("AGENT_PAYOUT_FAILED"))
        );
    }
}
