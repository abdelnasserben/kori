package com.kori.application.usecase;

import com.kori.application.command.CompleteClientRefundCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.CompleteClientRefundUseCase;
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

public class CompleteClientRefundService implements CompleteClientRefundUseCase {

    private final AdminAccessService adminAccessService;
    private final TimeProviderPort timeProviderPort;
    private final ClientRefundRepositoryPort clientRefundRepositoryPort;
    private final LedgerAppendPort ledgerAppendPort;
    private final AuditPort auditPort;

    public CompleteClientRefundService(
            AdminAccessService adminAccessService,
            TimeProviderPort timeProviderPort,
            ClientRefundRepositoryPort clientRefundRepositoryPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort) {
        this.adminAccessService = adminAccessService;
        this.timeProviderPort = timeProviderPort;
        this.clientRefundRepositoryPort = clientRefundRepositoryPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.auditPort = auditPort;
    }

    @Override
    public FinalizationResult execute(CompleteClientRefundCommand cmd) {
        adminAccessService.requireActiveAdmin(cmd.actorContext(), "complete client refund");

        ClientRefund refund = clientRefundRepositoryPort.findById(ClientRefundId.of(cmd.refundId()))
                .orElseThrow(() -> new NotFoundException("Client refund not found"));

        if (refund.status() == ClientRefundStatus.COMPLETED) {
            return FinalizationResult.ALREADY_APPLIED;
        }

        if (refund.status() != ClientRefundStatus.REQUESTED) {
            throw new ForbiddenOperationException("Client refund is not REQUESTED");
        }

        Instant now = timeProviderPort.now();
        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(refund.transactionId(), LedgerAccountRef.platformClientRefundClearing(), refund.amount()),
                LedgerEntry.credit(refund.transactionId(), LedgerAccountRef.platformBank(), refund.amount())
        ));

        refund.complete(now);
        clientRefundRepositoryPort.save(refund);

        auditPort.publish(AuditBuilder.buildBasicAudit(
                "CLIENT_REFUND_COMPLETED",
                cmd.actorContext(),
                now,
                Map.of("refundId", refund.id().value().toString())
        ));

        return FinalizationResult.APPLIED;
    }
}
