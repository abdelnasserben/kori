package com.kori.application.usecase;

import com.kori.application.command.CashInByAgentCommand;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorGuards;
import com.kori.application.guard.OperationStatusGuards;
import com.kori.application.port.in.CashInByAgentUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.CashInByAgentResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.application.utils.UuidParser;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentId;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CashInByAgentService implements CashInByAgentUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;
    private final IdGeneratorPort idGeneratorPort;
    private final AgentRepositoryPort agentRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final LedgerAppendPort ledgerAppendPort;
    private final AuditPort auditPort;
    private final OperationStatusGuards operationStatusGuards;

    public CashInByAgentService(TimeProviderPort timeProviderPort,
                                IdempotencyPort idempotencyPort,
                                IdGeneratorPort idGeneratorPort,
                                AgentRepositoryPort agentRepositoryPort,
                                ClientRepositoryPort clientRepositoryPort,
                                CardRepositoryPort cardRepositoryPort,
                                LedgerQueryPort ledgerQueryPort,
                                TransactionRepositoryPort transactionRepositoryPort,
                                LedgerAppendPort ledgerAppendPort,
                                AuditPort auditPort,
                                OperationStatusGuards operationStatusGuards) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.idGeneratorPort = idGeneratorPort;
        this.agentRepositoryPort = agentRepositoryPort;
        this.clientRepositoryPort = clientRepositoryPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.auditPort = auditPort;
        this.operationStatusGuards = operationStatusGuards;
    }

    @Override
    public CashInByAgentResult execute(CashInByAgentCommand command) {
        var cached = idempotencyPort.find(
                command.idempotencyKey(),
                command.idempotencyRequestHash(),
                CashInByAgentResult.class
        );
        if (cached.isPresent()) {
            return cached.get();
        }

        ActorGuards.requireAgent(command.actorContext(), "initiate cash-in");

        AgentId agentId = new AgentId(UuidParser.parse(command.actorContext().actorId(), "agentId"));
        Agent agent = agentRepositoryPort.findById(agentId)
                .orElseThrow(() -> new NotFoundException("Agent not found"));

        operationStatusGuards.requireActiveAgent(agent);

        Client client = clientRepositoryPort.findByPhoneNumber(command.clientPhoneNumber())
                .orElseThrow(() -> new NotFoundException("Client not found"));

        operationStatusGuards.requireActiveClient(client);

        Money amount = Money.positive(command.amount());
        Instant now = timeProviderPort.now();

        TransactionId txId = new TransactionId(idGeneratorPort.newUuid());
        Transaction tx = Transaction.cashInByAgent(txId, amount, now);
        tx = transactionRepositoryPort.save(tx);

        // Cash-in is free (no fees, no commissions)
        var clearingAcc = LedgerAccountRef.platformClearing();
        var clientAcc = LedgerAccountRef.client(client.id().value().toString());

        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(tx.id(), clearingAcc, amount),
                LedgerEntry.credit(tx.id(), clientAcc, amount)
        ));

        Map<String, String> metadata = new HashMap<>();
        metadata.put("transactionId", tx.id().value().toString());
        metadata.put("agentId", agent.id().value().toString());
        metadata.put("clientId", client.id().value().toString());
        metadata.put("clientPhoneNumber", client.phoneNumber());
        metadata.put("amount", amount.asBigDecimal().toPlainString());

        auditPort.publish(AuditBuilder.buildBasicAudit(
                "AGENT_CASH_IN",
                command.actorContext(),
                now,
                metadata
        ));

        CashInByAgentResult result = new CashInByAgentResult(
                tx.id().value().toString(),
                agent.id().value().toString(),
                client.id().value().toString(),
                client.phoneNumber(),
                amount.asBigDecimal()
        );

        idempotencyPort.save(command.idempotencyKey(), command.idempotencyRequestHash(), result);
        return result;
    }
}
