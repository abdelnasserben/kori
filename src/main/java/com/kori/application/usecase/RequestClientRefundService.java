package com.kori.application.usecase;

import com.kori.application.command.RequestClientRefundCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorStatusGuards;
import com.kori.application.idempotency.IdempotencyExecutor;
import com.kori.application.port.in.RequestClientRefundUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.ClientRefundResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.ClientCode;
import com.kori.domain.model.clientrefund.ClientRefund;
import com.kori.domain.model.clientrefund.ClientRefundId;
import com.kori.domain.model.clientrefund.ClientRefundStatus;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestClientRefundService implements RequestClientRefundUseCase {

    private final AdminAccessService adminAccessService;
    private final TimeProviderPort timeProviderPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final LedgerAppendPort ledgerAppendPort;
    private final LedgerQueryPort ledgerQueryPort;
    private final LedgerAccountLockPort ledgerAccountLockPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final ClientRefundRepositoryPort clientRefundRepositoryPort;
    private final AuditPort auditPort;
    private final IdGeneratorPort idGeneratorPort;
    private final IdempotencyExecutor idempotencyExecutor;

    public RequestClientRefundService(AdminAccessService adminAccessService, TimeProviderPort timeProviderPort,
                                      IdempotencyPort idempotencyPort,
                                      ClientRepositoryPort clientRepositoryPort,
                                      LedgerAppendPort ledgerAppendPort,
                                      LedgerQueryPort ledgerQueryPort, LedgerAccountLockPort ledgerAccountLockPort,
                                      TransactionRepositoryPort transactionRepositoryPort,
                                      ClientRefundRepositoryPort clientRefundRepositoryPort,
                                      AuditPort auditPort,
                                      IdGeneratorPort idGeneratorPort) {
        this.adminAccessService = adminAccessService;
        this.timeProviderPort = timeProviderPort;
        this.clientRepositoryPort = clientRepositoryPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.ledgerQueryPort = ledgerQueryPort;
        this.ledgerAccountLockPort = ledgerAccountLockPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.clientRefundRepositoryPort = clientRefundRepositoryPort;
        this.auditPort = auditPort;
        this.idGeneratorPort = idGeneratorPort;
        this.idempotencyExecutor = new IdempotencyExecutor(idempotencyPort);
    }

    @Override
    public ClientRefundResult execute(RequestClientRefundCommand cmd) {
        return idempotencyExecutor.execute(
                cmd.idempotencyKey(),
                cmd.idempotencyRequestHash(),
                ClientRefundResult.class,
                () -> {

                    adminAccessService.requireActiveAdmin(cmd.actorContext(), "initiate client refund");

                    ClientCode clientCode = ClientCode.of(cmd.clientCode());
                    Client client = clientRepositoryPort.findByCode(clientCode)
                            .orElseThrow(() -> new NotFoundException("Client not found"));
                    ActorStatusGuards.requireActiveClient(client);

                    if (clientRefundRepositoryPort.existsRequestedForClient(client.id())) {
                        throw new ForbiddenOperationException("A refund is already REQUESTED for this client");
                    }

                    var clientWallet = LedgerAccountRef.client(client.id().value().toString());
                    ledgerAccountLockPort.lock(clientWallet);
                    Money due = ledgerQueryPort.netBalance(clientWallet);
                    if (due.isZero()) {
                        throw new ForbiddenOperationException("No refund due for client");
                    }

                    Instant now = timeProviderPort.now();

                    TransactionId txId = new TransactionId(idGeneratorPort.newUuid());
                    Transaction tx = Transaction.clientRefund(txId, due, now);
                    transactionRepositoryPort.save(tx);

                    var refundClearing = LedgerAccountRef.platformClientRefundClearing();
                    ledgerAppendPort.append(List.of(
                            LedgerEntry.debit(tx.id(), clientWallet, due),
                            LedgerEntry.credit(tx.id(), refundClearing, due)
                    ));

                    ClientRefundId refundId = new ClientRefundId(idGeneratorPort.newUuid());
                    ClientRefund refund = ClientRefund.requested(refundId, client.id(), tx.id(), due, now);
                    clientRefundRepositoryPort.save(refund);

                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("transactionId", tx.id().value().toString());
                    metadata.put("refundId", refund.id().value().toString());
                    metadata.put("clientCode", cmd.clientCode());

                    auditPort.publish(AuditBuilder.buildBasicAudit(
                            "CLIENT_REFUND_REQUESTED",
                            cmd.actorContext(),
                            now,
                            metadata
                    ));

                    return new ClientRefundResult(
                            tx.id().value().toString(),
                            refund.id().value().toString(),
                            cmd.clientCode(),
                            due.asBigDecimal(),
                            ClientRefundStatus.REQUESTED.name()
                    );
                }
        );
    }
}
