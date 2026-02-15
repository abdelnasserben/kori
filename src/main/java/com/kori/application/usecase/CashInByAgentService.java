package com.kori.application.usecase;

import com.kori.application.command.CashInByAgentCommand;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorTypeGuards;
import com.kori.application.guard.AgentCashLimitGuard;
import com.kori.application.idempotency.IdempotencyExecutor;
import com.kori.application.port.in.CashInByAgentUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.CashInByAgentResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.PhoneNumber;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CashInByAgentService implements CashInByAgentUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdGeneratorPort idGeneratorPort;
    private final AgentRepositoryPort agentRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final AgentCashLimitGuard agentCashLimitGuard;
    private final LedgerAppendPort ledgerAppendPort;
    private final AuditPort auditPort;
    private final OperationAuthorizationService operationAuthorizationService;
    private final IdempotencyExecutor idempotencyExecutor;

    public CashInByAgentService(TimeProviderPort timeProviderPort,
                                IdempotencyPort idempotencyPort,
                                IdGeneratorPort idGeneratorPort,
                                AgentRepositoryPort agentRepositoryPort,
                                ClientRepositoryPort clientRepositoryPort,
                                LedgerQueryPort ledgerQueryPort,
                                PlatformConfigPort platformConfigPort,
                                TransactionRepositoryPort transactionRepositoryPort,
                                LedgerAppendPort ledgerAppendPort,
                                AuditPort auditPort,
                                OperationAuthorizationService operationAuthorizationService) {
        this.timeProviderPort = timeProviderPort;
        this.idGeneratorPort = idGeneratorPort;
        this.agentRepositoryPort = agentRepositoryPort;
        this.clientRepositoryPort = clientRepositoryPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.agentCashLimitGuard = new AgentCashLimitGuard(ledgerQueryPort, platformConfigPort);
        this.ledgerAppendPort = ledgerAppendPort;
        this.auditPort = auditPort;
        this.operationAuthorizationService = operationAuthorizationService;
        this.idempotencyExecutor = new IdempotencyExecutor(idempotencyPort);
    }

    @Override
    public CashInByAgentResult execute(CashInByAgentCommand command) {
        return idempotencyExecutor.execute(
                command.idempotencyKey(),
                command.idempotencyRequestHash(),
                CashInByAgentResult.class,
                () -> {

                    ActorTypeGuards.onlyAgentCan(command.actorContext(), "initiate cash-in");

                    Agent agent = agentRepositoryPort.findByCode(AgentCode.of(command.actorContext().actorRef()))
                            .orElseThrow(() -> new NotFoundException("Agent not found"));

                    operationAuthorizationService.authorizeAgentOperation(agent);

                    Client client = clientRepositoryPort.findByPhoneNumber(PhoneNumber.of(command.clientPhoneNumber()))
                            .orElseThrow(() -> new NotFoundException("Client not found"));

                    operationAuthorizationService.authorizeClientPayment(client);

                    Money amount = Money.positive(command.amount());
                    Instant now = timeProviderPort.now();

                    TransactionId txId = new TransactionId(idGeneratorPort.newUuid());
                    Transaction tx = Transaction.cashInByAgent(txId, amount, now);
                    tx = transactionRepositoryPort.save(tx);

                    // Cash-in is free (no fees, no commissions)
                    agentRepositoryPort.findByIdForUpdate(agent.id());
                    agentCashLimitGuard.ensureProjectedBalanceWithinLimit(agent.id().value().toString(), amount, Money.zero());

                    var clearingAcc = LedgerAccountRef.agentCashClearing(agent.id().value().toString());
                    var clientAcc = LedgerAccountRef.client(client.id().value().toString());

                    ledgerAppendPort.append(List.of(
                            LedgerEntry.debit(tx.id(), clearingAcc, amount),
                            LedgerEntry.credit(tx.id(), clientAcc, amount)
                    ));

                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("transactionId", tx.id().value().toString());
                    metadata.put("clientCode", client.code().value());
                    metadata.put("amount", amount.asBigDecimal().toPlainString());

                    auditPort.publish(AuditBuilder.buildBasicAudit(
                            "AGENT_CASH_IN",
                            command.actorContext(),
                            now,
                            metadata
                    ));

                    return new CashInByAgentResult(
                            tx.id().value().toString(),
                            agent.id().value().toString(),
                            client.id().value().toString(),
                            client.phoneNumber().value(),
                            amount.asBigDecimal()
                    );
                }
        );
    }
}
