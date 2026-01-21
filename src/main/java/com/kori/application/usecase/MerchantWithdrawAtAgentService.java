package com.kori.application.usecase;

import com.kori.application.command.MerchantWithdrawAtAgentCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.MerchantWithdrawAtAgentUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.MerchantWithdrawAtAgentResult;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccount;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.Transaction;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MerchantWithdrawAtAgentService implements MerchantWithdrawAtAgentUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;

    private final MerchantRepositoryPort merchantRepositoryPort;
    private final AgentRepositoryPort agentRepositoryPort;

    private final FeePolicyPort feePolicyPort;
    private final CommissionPolicyPort commissionPolicyPort;

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final LedgerAppendPort ledgerAppendPort;
    private final AuditPort auditPort;

    public MerchantWithdrawAtAgentService(TimeProviderPort timeProviderPort,
                                          IdempotencyPort idempotencyPort,
                                          MerchantRepositoryPort merchantRepositoryPort,
                                          AgentRepositoryPort agentRepositoryPort,
                                          FeePolicyPort feePolicyPort,
                                          CommissionPolicyPort commissionPolicyPort,
                                          TransactionRepositoryPort transactionRepositoryPort,
                                          LedgerAppendPort ledgerAppendPort,
                                          AuditPort auditPort) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.merchantRepositoryPort = merchantRepositoryPort;
        this.agentRepositoryPort = agentRepositoryPort;
        this.feePolicyPort = feePolicyPort;
        this.commissionPolicyPort = commissionPolicyPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.auditPort = auditPort;
    }

    @Override
    public MerchantWithdrawAtAgentResult execute(MerchantWithdrawAtAgentCommand command) {
        var cached = idempotencyPort.find(command.idempotencyKey(), MerchantWithdrawAtAgentResult.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        // Initié par AGENT uniquement
        if (command.actorContext().actorType() != ActorType.AGENT) {
            throw new ForbiddenOperationException("Only AGENT can initiate MerchantWithdrawAtAgent");
        }

        // agent + merchant doivent exister
        if (!agentRepositoryPort.existsById(command.agentId())) {
            throw new ForbiddenOperationException("Agent not found");
        }
        merchantRepositoryPort.findById(command.merchantId())
                .orElseThrow(() -> new ForbiddenOperationException("Merchant not found"));

        Instant now = timeProviderPort.now();

        Money amount = Money.of(command.amount());
        Money fee = feePolicyPort.merchantWithdrawFee(amount);
        Money commission = commissionPolicyPort.merchantWithdrawAgentCommission(fee);

        // Règle métier: commission ≤ fee
        if (commission.isGreaterThan(fee)) {
            throw new ForbiddenOperationException("Invalid commission: commission cannot exceed fee");
        }

        Money platformRevenue = fee.minus(commission);
        Money totalDebitedMerchant = amount.plus(fee);

        Transaction tx = Transaction.merchantWithdrawAtAgent(amount, now);
        tx = transactionRepositoryPort.save(tx);

        // Ledger (spec)
        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(tx.id(), LedgerAccount.MERCHANT, totalDebitedMerchant, command.merchantId()),
                LedgerEntry.credit(tx.id(), LedgerAccount.PLATFORM_CLEARING, amount, null),
                LedgerEntry.credit(tx.id(), LedgerAccount.AGENT, commission, command.agentId()),
                LedgerEntry.credit(tx.id(), LedgerAccount.PLATFORM, platformRevenue, null)
        ));

        Map<String, String> metadata = new HashMap<>();
        metadata.put("merchantId", command.merchantId());
        metadata.put("agentId", command.agentId());
        metadata.put("transactionId", tx.id().value());

        auditPort.publish(new AuditEvent(
                "MERCHANT_WITHDRAW_AT_AGENT",
                command.actorContext().actorType().name(),
                command.actorContext().actorId(),
                now,
                metadata
        ));

        MerchantWithdrawAtAgentResult result = new MerchantWithdrawAtAgentResult(
                tx.id().value(),
                command.merchantId(),
                command.agentId(),
                amount.asBigDecimal(),
                fee.asBigDecimal(),
                commission.asBigDecimal(),
                totalDebitedMerchant.asBigDecimal()
        );

        idempotencyPort.save(command.idempotencyKey(), result);
        return result;
    }
}
