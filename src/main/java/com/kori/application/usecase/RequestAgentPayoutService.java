package com.kori.application.usecase;

import com.kori.application.command.RequestAgentPayoutCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorStatusGuards;
import com.kori.application.idempotency.IdempotencyExecutor;
import com.kori.application.port.in.RequestAgentPayoutUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.AgentPayoutResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.agent.AgentId;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.payout.Payout;
import com.kori.domain.model.payout.PayoutId;
import com.kori.domain.model.payout.PayoutStatus;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestAgentPayoutService implements RequestAgentPayoutUseCase {

    private final TimeProviderPort timeProviderPort;

    private final AdminAccessService adminAccessService;
    private final AgentRepositoryPort agentRepositoryPort;
    private final LedgerAppendPort ledgerAppendPort;
    private final LedgerQueryPort ledgerQueryPort;
    private final LedgerAccountLockPort ledgerAccountLockPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final PayoutRepositoryPort payoutRepositoryPort;
    private final AuditPort auditPort;
    private final IdGeneratorPort idGeneratorPort;
    private final IdempotencyExecutor idempotencyExecutor;

    public RequestAgentPayoutService(
            TimeProviderPort timeProviderPort, AdminAccessService adminAccessService,
            IdempotencyPort idempotencyPort,
            AgentRepositoryPort agentRepositoryPort, LedgerAppendPort ledgerAppendPort,
            LedgerQueryPort ledgerQueryPort, LedgerAccountLockPort ledgerAccountLockPort,
            TransactionRepositoryPort transactionRepositoryPort,
            PayoutRepositoryPort payoutRepositoryPort,
            AuditPort auditPort, IdGeneratorPort idGeneratorPort) {
        this.timeProviderPort = timeProviderPort;
        this.adminAccessService = adminAccessService;
        this.agentRepositoryPort = agentRepositoryPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.ledgerQueryPort = ledgerQueryPort;
        this.ledgerAccountLockPort = ledgerAccountLockPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.payoutRepositoryPort = payoutRepositoryPort;
        this.auditPort = auditPort;
        this.idGeneratorPort = idGeneratorPort;
        this.idempotencyExecutor = new IdempotencyExecutor(idempotencyPort);
    }

    @Override
    public AgentPayoutResult execute(RequestAgentPayoutCommand command) {
        return idempotencyExecutor.execute(
                command.idempotencyKey(),
                command.idempotencyRequestHash(),
                AgentPayoutResult.class,
                () -> {

                    adminAccessService.requireActiveAdmin(command.actorContext(), "initiate agent payout");

                    // Agent must exist and active
                    Agent agent = agentRepositoryPort.findByCode(AgentCode.of(command.agentCode()))
                            .orElseThrow(() -> new NotFoundException("Agent not found"));
                    ActorStatusGuards.requireActiveAgent(agent);
                    AgentId agentId = agent.id();

                    // Check existing agent request
                    if (payoutRepositoryPort.existsRequestedForAgent(agent.id())) {
                        throw new ForbiddenOperationException("A payout is already REQUESTED for this agent");
                    }

                    // Payout must compensate exactly what is due to the agent.
                    var agentWalletAcc = LedgerAccountRef.agentWallet(agentId.value().toString());
                    ledgerAccountLockPort.lock(agentWalletAcc);
                    Money due = ledgerQueryPort.netBalance(agentWalletAcc);

                    if (due.isZero()) {
                        throw new ForbiddenOperationException("No payout due for agent");
                    }

                    Instant now = timeProviderPort.now();

                    TransactionId txId = new TransactionId(idGeneratorPort.newUuid());
                    Transaction tx = Transaction.agentPayout(txId, due, now);
                    transactionRepositoryPort.save(tx);

                    var platformClearingAcc = LedgerAccountRef.platformClearing();
                    ledgerAppendPort.append(List.of(
                            LedgerEntry.debit(tx.id(), agentWalletAcc, due),
                            LedgerEntry.credit(tx.id(), platformClearingAcc, due)
                    ));

                    PayoutId payoutId = new PayoutId(idGeneratorPort.newUuid());
                    Payout payout = Payout.requested(payoutId, agentId, tx.id(), due, now);
                    payoutRepositoryPort.save(payout);

                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("transactionId", tx.id().value().toString());
                    metadata.put("payoutId", payout.id().value().toString());
                    metadata.put("agentCode", command.agentCode());

                    auditPort.publish(AuditBuilder.buildBasicAudit(
                            "AGENT_PAYOUT_REQUESTED",
                            command.actorContext(),
                            now,
                            metadata
                    ));

                    return new AgentPayoutResult(
                            tx.id().value().toString(),
                            payout.id().value().toString(),
                            command.agentCode(),
                            due.asBigDecimal(),
                            PayoutStatus.REQUESTED.name()
                    );
                }
        );
    }
}
