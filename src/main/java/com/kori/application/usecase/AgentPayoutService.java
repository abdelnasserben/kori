package com.kori.application.usecase;

import com.kori.application.command.AgentPayoutCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.AgentPayoutUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.AgentPayoutResult;
import com.kori.application.security.ActorType;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.payout.Payout;
import com.kori.domain.model.payout.PayoutStatus;
import com.kori.domain.model.transaction.Transaction;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class AgentPayoutService implements AgentPayoutUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;

    private final AgentRepositoryPort agentRepositoryPort;
    private final LedgerQueryPort ledgerQueryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final PayoutRepositoryPort payoutRepositoryPort;
    private final AuditPort auditPort;

    public AgentPayoutService(TimeProviderPort timeProviderPort,
                              IdempotencyPort idempotencyPort,
                              AgentRepositoryPort agentRepositoryPort,
                              LedgerQueryPort ledgerQueryPort,
                              TransactionRepositoryPort transactionRepositoryPort,
                              PayoutRepositoryPort payoutRepositoryPort,
                              AuditPort auditPort) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.agentRepositoryPort = agentRepositoryPort;
        this.ledgerQueryPort = ledgerQueryPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.payoutRepositoryPort = payoutRepositoryPort;
        this.auditPort = auditPort;
    }

    @Override
    public AgentPayoutResult execute(AgentPayoutCommand command) {
        var cached = idempotencyPort.find(command.idempotencyKey(), AgentPayoutResult.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        // ADMIN only
        if (command.actorContext().actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can initiate agent payout");
        }

        if (!agentRepositoryPort.existsById(command.agentId())) {
            throw new ForbiddenOperationException("Agent not found");
        }

        if (payoutRepositoryPort.existsRequestedForAgent(command.agentId())) {
            throw new ForbiddenOperationException("A payout is already REQUESTED for this agent");
        }

        // Spec: payout must compensate exactly what is due to the agent.
        Money due = ledgerQueryPort.agentAvailableBalance(command.agentId());
        if (due.isZero()) {
            throw new ForbiddenOperationException("No payout due for agent");
        }

        Instant now = timeProviderPort.now();

        Transaction tx = Transaction.agentPayout(due, now);
        tx = transactionRepositoryPort.save(tx);

        Payout payout = Payout.requested(command.agentId(), tx.id(), due, now);
        payout = payoutRepositoryPort.save(payout);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("agentId", command.agentId());
        metadata.put("transactionId", tx.id().value());
        metadata.put("payoutId", payout.id().value().toString());

        auditPort.publish(new AuditEvent(
                "AGENT_PAYOUT_REQUESTED",
                command.actorContext().actorType().name(),
                command.actorContext().actorId(),
                now,
                metadata
        ));

        AgentPayoutResult result = new AgentPayoutResult(
                tx.id().value(),
                payout.id().value().toString(),
                command.agentId(),
                due.asBigDecimal(),
                PayoutStatus.REQUESTED
        );

        idempotencyPort.save(command.idempotencyKey(), result);
        return result;
    }
}
