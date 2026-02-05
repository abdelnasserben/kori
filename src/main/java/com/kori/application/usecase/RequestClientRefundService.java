package com.kori.application.usecase;

import com.kori.application.command.RequestClientRefundCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorGuards;
import com.kori.application.port.in.RequestClientRefundUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.ClientRefundResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.clientrefund.ClientRefund;
import com.kori.domain.model.clientrefund.ClientRefundId;
import com.kori.domain.model.clientrefund.ClientRefundStatus;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestClientRefundService implements RequestClientRefundUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final LedgerAppendPort ledgerAppendPort;
    private final LedgerQueryPort ledgerQueryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final ClientRefundRepositoryPort clientRefundRepositoryPort;
    private final AuditPort auditPort;
    private final IdGeneratorPort idGeneratorPort;

    public RequestClientRefundService(TimeProviderPort timeProviderPort,
                                      IdempotencyPort idempotencyPort,
                                      ClientRepositoryPort clientRepositoryPort,
                                      LedgerAppendPort ledgerAppendPort,
                                      LedgerQueryPort ledgerQueryPort,
                                      TransactionRepositoryPort transactionRepositoryPort,
                                      ClientRefundRepositoryPort clientRefundRepositoryPort,
                                      AuditPort auditPort,
                                      IdGeneratorPort idGeneratorPort) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.clientRepositoryPort = clientRepositoryPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.ledgerQueryPort = ledgerQueryPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.clientRefundRepositoryPort = clientRefundRepositoryPort;
        this.auditPort = auditPort;
        this.idGeneratorPort = idGeneratorPort;
    }

    @Override
    public ClientRefundResult execute(RequestClientRefundCommand cmd) {
        var cached = idempotencyPort.find(cmd.idempotencyKey(), cmd.idempotencyRequestHash(), ClientRefundResult.class);
        if (cached.isPresent()) return cached.get();

        ActorGuards.requireAdmin(cmd.actorContext(), "initiate client refund");

        ClientId clientId = ClientId.of(cmd.clientId());
        Client client = clientRepositoryPort.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Client not found"));

        if (client.status() != Status.ACTIVE) {
            throw new ForbiddenOperationException("Client is not active");
        }

        if (clientRefundRepositoryPort.existsRequestedForClient(clientId)) {
            throw new ForbiddenOperationException("A refund is already REQUESTED for this client");
        }

        Money due = ledgerQueryPort.netBalance(LedgerAccountRef.client(clientId.value().toString()));
        if (due.isZero()) {
            throw new ForbiddenOperationException("No refund due for client");
        }

        Instant now = timeProviderPort.now();
        TransactionId txId = new TransactionId(idGeneratorPort.newUuid());
        Transaction tx = Transaction.clientRefund(txId, due, now);
        transactionRepositoryPort.save(tx);

        var clientWallet = LedgerAccountRef.client(clientId.value().toString());
        var refundClearing = LedgerAccountRef.platformClientRefundClearing();
        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(tx.id(), clientWallet, due),
                LedgerEntry.credit(tx.id(), refundClearing, due)
        ));

        ClientRefundId refundId = new ClientRefundId(idGeneratorPort.newUuid());
        ClientRefund refund = ClientRefund.requested(refundId, clientId, tx.id(), due, now);
        clientRefundRepositoryPort.save(refund);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("transactionId", tx.id().value().toString());
        metadata.put("clientId", cmd.clientId());
        metadata.put("refundId", refund.id().value().toString());

        auditPort.publish(AuditBuilder.buildBasicAudit(
                "CLIENT_REFUND_REQUESTED",
                cmd.actorContext(),
                now,
                metadata
        ));

        var result = new ClientRefundResult(
                tx.id().value().toString(),
                refund.id().value().toString(),
                cmd.clientId(),
                due.asBigDecimal(),
                ClientRefundStatus.REQUESTED.name()
        );
        idempotencyPort.save(cmd.idempotencyKey(), cmd.idempotencyRequestHash(), result);
        return result;
    }
}
