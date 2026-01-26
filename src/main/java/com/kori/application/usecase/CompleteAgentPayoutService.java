package com.kori.application.usecase;

import com.kori.application.command.CompleteAgentPayoutCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.CompleteAgentPayoutUseCase;
import com.kori.application.port.out.*;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.payout.Payout;
import com.kori.domain.model.payout.PayoutId;
import com.kori.domain.model.payout.PayoutStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class CompleteAgentPayoutService implements CompleteAgentPayoutUseCase {

    private final TimeProviderPort timeProviderPort;
    private final PayoutRepositoryPort payoutRepositoryPort;
    private final LedgerAppendPort ledgerAppendPort;
    private final AuditPort auditPort;

    public CompleteAgentPayoutService(
            TimeProviderPort timeProviderPort,
            PayoutRepositoryPort payoutRepositoryPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort) {
        this.timeProviderPort = timeProviderPort;
        this.payoutRepositoryPort = payoutRepositoryPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.auditPort = auditPort;
    }

    @Override
    public void execute(CompleteAgentPayoutCommand command) {
        if (command.actorContext().actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can complete agent payout");
        }

        Payout payout = payoutRepositoryPort.findById(PayoutId.of(command.payoutId()))
                .orElseThrow(() -> new NotFoundException("Payout not found"));

        if (payout.status() != PayoutStatus.REQUESTED) {
            throw new ForbiddenOperationException("Payout is not REQUESTED");
        }

        Instant now = timeProviderPort.now();

        var agentAcc = LedgerAccountRef.agent(payout.agentId().toString());
        var clearingAcc = LedgerAccountRef.platformClearing();

        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(payout.transactionId(), agentAcc, payout.amount()),
                LedgerEntry.credit(payout.transactionId(), clearingAcc, payout.amount())
        ));

        payout.complete(now);
        payoutRepositoryPort.save(payout);

        auditPort.publish(new AuditEvent(
                "AGENT_PAYOUT_COMPLETED",
                command.actorContext().actorType().name(),
                command.actorContext().actorId(),
                now,
                Map.of("payoutId", payout.id().toString())
        ));
    }
}
