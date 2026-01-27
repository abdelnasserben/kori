package com.kori.application.usecase;

import com.kori.application.command.RequestAgentPayoutCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.RequestAgentPayoutUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.AgentPayoutResult;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.agent.AgentId;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.payout.Payout;
import com.kori.domain.model.payout.PayoutStatus;
import com.kori.domain.model.transaction.Transaction;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class RequestAgentPayoutService implements RequestAgentPayoutUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;

    private final AgentRepositoryPort agentRepositoryPort;
    private final LedgerQueryPort ledgerQueryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final PayoutRepositoryPort payoutRepositoryPort;
    private final AuditPort auditPort;

    public RequestAgentPayoutService(
            TimeProviderPort timeProviderPort,
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
    public AgentPayoutResult execute(RequestAgentPayoutCommand command) {
        var cached = idempotencyPort.find(command.idempotencyKey(), AgentPayoutResult.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        // ADMIN only
        if (command.actorContext().actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can initiate agent payout");
        }

        // Agent must exist and active
        Agent agent = agentRepositoryPort.findByCode(AgentCode.of(command.agentCode()))
                .orElseThrow(() -> new ForbiddenOperationException("Agent not found"));

        if (!agent.status().equals(Status.ACTIVE)) {
            throw new ForbiddenOperationException("Agent is not active");
        }
        AgentId agentId = agent.id();

        // Check existing agent request
        if (payoutRepositoryPort.existsRequestedForAgent(agent.id())) {
            throw new ForbiddenOperationException("A payout is already REQUESTED for this agent");
        }

        // Spec: payout must compensate exactly what is due to the agent.
        Money due = ledgerQueryPort.netBalance(LedgerAccountRef.agent(agentId.toString()));
        if (due.isZero()) {
            throw new ForbiddenOperationException("No payout due for agent");
        }

        Instant now = timeProviderPort.now();

        Transaction tx = Transaction.agentPayout(due, now);
        tx = transactionRepositoryPort.save(tx);

        Payout payout = Payout.requested(agentId, tx.id(), due, now);
        payoutRepositoryPort.save(payout);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("transactionId", tx.id().toString());
        metadata.put("agentCode", command.agentCode());
        metadata.put("payoutId", payout.id().toString());

        auditPort.publish(new AuditEvent(
                "AGENT_PAYOUT_REQUESTED",
                command.actorContext().actorType().name(),
                command.actorContext().actorId(),
                now,
                metadata
        ));

        AgentPayoutResult result = new AgentPayoutResult(
                tx.id().toString(),
                payout.id().toString(),
                command.agentCode(),
                due.asBigDecimal(),
                PayoutStatus.REQUESTED.name()
        );

        idempotencyPort.save(command.idempotencyKey(), result);
        return result;
    }
}
