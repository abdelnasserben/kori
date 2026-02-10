package com.kori.application.usecase;

import com.kori.application.command.FailClientRefundCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorGuards;
import com.kori.application.port.in.FailClientRefundUseCase;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.ClientRefundRepositoryPort;
import com.kori.application.port.out.LedgerAppendPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.FinalizationResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.clientrefund.ClientRefund;
import com.kori.domain.model.clientrefund.ClientRefundId;
import com.kori.domain.model.clientrefund.ClientRefundStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class FailClientRefundService implements FailClientRefundUseCase {

    private final TimeProviderPort timeProviderPort;
    private final ClientRefundRepositoryPort clientRefundRepositoryPort;
    private final LedgerAppendPort ledgerAppendPort;
    private final AuditPort auditPort;

    public FailClientRefundService(TimeProviderPort timeProviderPort,
                                   ClientRefundRepositoryPort clientRefundRepositoryPort,
                                   LedgerAppendPort ledgerAppendPort,
                                   AuditPort auditPort) {
        this.timeProviderPort = timeProviderPort;
        this.clientRefundRepositoryPort = clientRefundRepositoryPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.auditPort = auditPort;
    }

    @Override
    public FinalizationResult execute(FailClientRefundCommand cmd) {
        ActorGuards.requireAdmin(cmd.actorContext(), "fail client refund");

        ClientRefund refund = clientRefundRepositoryPort.findById(ClientRefundId.of(cmd.refundId()))
                .orElseThrow(() -> new NotFoundException("Client refund not found"));

        if (refund.status() == ClientRefundStatus.FAILED) {
            return FinalizationResult.ALREADY_APPLIED;
        }

        if (refund.status() != ClientRefundStatus.REQUESTED) {
            throw new ForbiddenOperationException("Client refund is not in REQUESTED state");
        }

        Instant now = timeProviderPort.now();
        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(refund.transactionId(), LedgerAccountRef.platformClientRefundClearing(), refund.amount()),
                LedgerEntry.credit(refund.transactionId(), LedgerAccountRef.client(refund.clientId().value().toString()), refund.amount())
        ));

        refund.fail(now, cmd.reason());
        clientRefundRepositoryPort.save(refund);

        auditPort.publish(AuditBuilder.buildBasicAudit(
                "CLIENT_REFUND_FAILED",
                cmd.actorContext(),
                now,
                Map.of("refundId", refund.id().value().toString(), "reason", cmd.reason())
        ));

        return FinalizationResult.APPLIED;
    }
}
