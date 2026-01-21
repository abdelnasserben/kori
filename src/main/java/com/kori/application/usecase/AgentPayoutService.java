package com.kori.application.usecase;

import com.kori.application.command.AgentPayoutCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.AgentPayoutUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.AgentPayoutResult;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccount;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.Transaction;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AgentPayoutService implements AgentPayoutUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;

    private final AgentRepositoryPort agentRepositoryPort;
    private final LedgerQueryPort ledgerQueryPort;
    private final LedgerAppendPort ledgerAppendPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final AuditPort auditPort;

    public AgentPayoutService(TimeProviderPort timeProviderPort,
                              IdempotencyPort idempotencyPort,
                              AgentRepositoryPort agentRepositoryPort,
                              LedgerQueryPort ledgerQueryPort,
                              LedgerAppendPort ledgerAppendPort,
                              TransactionRepositoryPort transactionRepositoryPort,
                              AuditPort auditPort) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.agentRepositoryPort = agentRepositoryPort;
        this.ledgerQueryPort = ledgerQueryPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
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

        Money requested = Money.of(command.amount());
        Money available = ledgerQueryPort.agentAvailableBalance(command.agentId());

        if (requested.isGreaterThan(available)) {
            throw new ForbiddenOperationException("Payout amount exceeds agent balance");
        }

        Instant now = timeProviderPort.now();

        Transaction tx = Transaction.agentPayout(requested, now);
        tx = transactionRepositoryPort.save(tx);

        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(tx.id(), LedgerAccount.AGENT, requested, command.agentId()),
                LedgerEntry.credit(tx.id(), LedgerAccount.PLATFORM_CLEARING, requested, null)
        ));

        Map<String, String> metadata = new HashMap<>();
        metadata.put("agentId", command.agentId());
        metadata.put("transactionId", tx.id().value());

        auditPort.publish(new AuditEvent(
                "AGENT_PAYOUT",
                command.actorContext().actorType().name(),
                command.actorContext().actorId(),
                now,
                metadata
        ));

        AgentPayoutResult result = new AgentPayoutResult(
                tx.id().value(),
                command.agentId(),
                requested.asBigDecimal()
        );

        idempotencyPort.save(command.idempotencyKey(), result);
        return result;
    }
}
