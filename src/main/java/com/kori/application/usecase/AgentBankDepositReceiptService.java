package com.kori.application.usecase;

import com.kori.application.command.AgentBankDepositReceiptCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorGuards;
import com.kori.application.idempotency.IdempotencyExecutor;
import com.kori.application.port.in.AgentBankDepositReceiptUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.AgentBankDepositReceiptResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AgentBankDepositReceiptService implements AgentBankDepositReceiptUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdGeneratorPort idGeneratorPort;
    private final AgentRepositoryPort agentRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final LedgerAppendPort ledgerAppendPort;
    private final AuditPort auditPort;
    private final IdempotencyExecutor idempotencyExecutor;

    public AgentBankDepositReceiptService(TimeProviderPort timeProviderPort,
                                          IdempotencyPort idempotencyPort,
                                          IdGeneratorPort idGeneratorPort,
                                          AgentRepositoryPort agentRepositoryPort,
                                          TransactionRepositoryPort transactionRepositoryPort,
                                          LedgerAppendPort ledgerAppendPort,
                                          AuditPort auditPort) {
        this.timeProviderPort = timeProviderPort;
        this.idGeneratorPort = idGeneratorPort;
        this.agentRepositoryPort = agentRepositoryPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.auditPort = auditPort;
        this.idempotencyExecutor = new IdempotencyExecutor(idempotencyPort);
    }

    @Override
    public AgentBankDepositReceiptResult execute(AgentBankDepositReceiptCommand command) {
        return idempotencyExecutor.execute(
                command.idempotencyKey(),
                command.idempotencyRequestHash(),
                AgentBankDepositReceiptResult.class,
                () -> {
                    // business logic

                    ActorGuards.requireAdmin(command.actorContext(), "record bank deposit receipt");

                    var agent = agentRepositoryPort.findByCode(AgentCode.of(command.agentCode()))
                            .orElseThrow(() -> new NotFoundException("Agent not found"));

                    if (agent.status() != Status.ACTIVE) {
                        throw new ForbiddenOperationException("Agent is not active");
                    }

                    Money amount = Money.positive(command.amount());
                    Instant now = timeProviderPort.now();

                    agentRepositoryPort.findByIdForUpdate(agent.id());

                    TransactionId txId = new TransactionId(idGeneratorPort.newUuid());
                    Transaction tx = Transaction.agentBankDepositReceipt(txId, amount, now);
                    transactionRepositoryPort.save(tx);

                    var bankAcc = LedgerAccountRef.platformBank();
                    var agentCashClearingAcc = LedgerAccountRef.agentCashClearing(agent.id().value().toString());

                    ledgerAppendPort.append(List.of(
                            LedgerEntry.debit(tx.id(), bankAcc, amount),
                            LedgerEntry.credit(tx.id(), agentCashClearingAcc, amount)
                    ));

                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("transactionId", tx.id().value().toString());
                    metadata.put("agentCode", agent.id().value().toString());
                    metadata.put("agentCode", agent.code().value());
                    metadata.put("amount", amount.asBigDecimal().toPlainString());

                    auditPort.publish(AuditBuilder.buildBasicAudit(
                            "AGENT_BANK_DEPOSIT_RECEIPT",
                            command.actorContext(),
                            now,
                            metadata
                    ));

                    return new AgentBankDepositReceiptResult(
                            tx.id().value().toString(),
                            agent.code().value(),
                            amount.asBigDecimal()
                    );
                }
        );
    }
}
