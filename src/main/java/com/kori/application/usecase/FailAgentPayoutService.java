package com.kori.application.usecase;

import com.kori.application.command.FailAgentPayoutCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.FailAgentPayoutUseCase;
import com.kori.application.port.out.AuditEvent;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.PayoutRepositoryPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.security.ActorType;
import com.kori.domain.model.payout.Payout;
import com.kori.domain.model.payout.PayoutId;
import com.kori.domain.model.payout.PayoutStatus;

import java.time.Instant;
import java.util.Map;

public final class FailAgentPayoutService implements FailAgentPayoutUseCase {

    private final TimeProviderPort timeProviderPort;
    private final PayoutRepositoryPort payoutRepositoryPort;
    private final AuditPort auditPort;

    public FailAgentPayoutService(TimeProviderPort timeProviderPort,
                                  PayoutRepositoryPort payoutRepositoryPort,
                                  AuditPort auditPort) {
        this.timeProviderPort = timeProviderPort;
        this.payoutRepositoryPort = payoutRepositoryPort;
        this.auditPort = auditPort;
    }

    @Override
    public void execute(FailAgentPayoutCommand command) {
        if (command.actorContext().actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can fail payouts");
        }

        Payout payout = payoutRepositoryPort.findById(PayoutId.of(command.payoutId()))
                .orElseThrow(() -> new NotFoundException("Payout not found"));

        if (payout.status() != PayoutStatus.REQUESTED) {
            throw new ForbiddenOperationException("Payout is not in REQUESTED code");
        }

        Instant now = timeProviderPort.now();
        payout.fail(now, command.reason());
        payoutRepositoryPort.save(payout);

        auditPort.publish(new AuditEvent(
                "AGENT_PAYOUT_FAILED",
                command.actorContext().actorType().name(),
                command.actorContext().actorId(),
                now,
                Map.of("payoutId", payout.id().toString(), "reason", command.reason())
        ));
    }
}
