package com.kori.application.usecase;

import com.kori.application.command.CompleteAgentPayoutCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.CompleteAgentPayoutUseCase;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.LedgerAppendPort;
import com.kori.application.port.out.PayoutRepositoryPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.FinalizationResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.payout.Payout;
import com.kori.domain.model.payout.PayoutId;
import com.kori.domain.model.payout.PayoutStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class CompleteAgentPayoutService implements CompleteAgentPayoutUseCase {

    private final AdminAccessService adminAccessService;
    private final TimeProviderPort timeProviderPort;
    private final PayoutRepositoryPort payoutRepositoryPort;
    private final LedgerAppendPort ledgerAppendPort;
    private final AuditPort auditPort;

    public CompleteAgentPayoutService(
            AdminAccessService adminAccessService,
            TimeProviderPort timeProviderPort,
            PayoutRepositoryPort payoutRepositoryPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort) {
        this.adminAccessService = adminAccessService;
        this.timeProviderPort = timeProviderPort;
        this.payoutRepositoryPort = payoutRepositoryPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.auditPort = auditPort;
    }

    @Override
    public FinalizationResult execute(CompleteAgentPayoutCommand command) {
        adminAccessService.requireActiveAdmin(command.actorContext(), "complete agent payout");

        Payout payout = payoutRepositoryPort.findById(PayoutId.of(command.payoutId()))
                .orElseThrow(() -> new NotFoundException("Payout not found"));

        if (payout.status() == PayoutStatus.COMPLETED) {
            return FinalizationResult.ALREADY_APPLIED;
        }

        if (payout.status() != PayoutStatus.REQUESTED) {
            throw new ForbiddenOperationException("Payout is not REQUESTED");
        }

        Instant now = timeProviderPort.now();

        var clearingAcc = LedgerAccountRef.platformClearing();
        var bankAcc = LedgerAccountRef.platformBank();

        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(payout.transactionId(), clearingAcc, payout.amount()),
                LedgerEntry.credit(payout.transactionId(), bankAcc, payout.amount())
        ));

        payout.complete(now);
        payoutRepositoryPort.save(payout);

        auditPort.publish(AuditBuilder.buildBasicAudit(
                "AGENT_PAYOUT_COMPLETED",
                command.actorContext(),
                now,
                Map.of("payoutId", payout.id().value().toString())
        ));
        return FinalizationResult.APPLIED;
    }
}
