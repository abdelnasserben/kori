package com.kori.application.usecase;

import com.kori.application.command.MerchantWithdrawAtAgentCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.InsufficientFundsException;
import com.kori.application.port.in.MerchantWithdrawAtAgentUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.MerchantWithdrawAtAgentResult;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.merchant.MerchantCode;
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
    private final AccountProfilePort accountProfilePort;

    private final FeePolicyPort feePolicyPort;
    private final CommissionPolicyPort commissionPolicyPort;

    private final LedgerQueryPort ledgerQueryPort;

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final LedgerAppendPort ledgerAppendPort;
    private final AuditPort auditPort;

    public MerchantWithdrawAtAgentService(TimeProviderPort timeProviderPort,
                                          IdempotencyPort idempotencyPort,
                                          MerchantRepositoryPort merchantRepositoryPort,
                                          AgentRepositoryPort agentRepositoryPort,
                                          AccountProfilePort accountProfilePort,
                                          FeePolicyPort feePolicyPort,
                                          CommissionPolicyPort commissionPolicyPort,
                                          LedgerQueryPort ledgerQueryPort,
                                          TransactionRepositoryPort transactionRepositoryPort,
                                          LedgerAppendPort ledgerAppendPort,
                                          AuditPort auditPort) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.merchantRepositoryPort = merchantRepositoryPort;
        this.agentRepositoryPort = agentRepositoryPort;
        this.accountProfilePort = accountProfilePort;
        this.feePolicyPort = feePolicyPort;
        this.commissionPolicyPort = commissionPolicyPort;
        this.ledgerQueryPort = ledgerQueryPort;
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

        // Resolve AGENT by code
        Agent agent = agentRepositoryPort.findByCode(AgentCode.of(command.agentCode()))
                .orElseThrow(() -> new ForbiddenOperationException("Agent not found"));

        // Resolve MERCHANT by code
        Merchant merchant = merchantRepositoryPort.findByCode(MerchantCode.of(command.merchantCode()))
                .orElseThrow(() -> new ForbiddenOperationException("Merchant not found"));

        // Profiles must be ACTIVE (recommended)
        var agentAcc = LedgerAccountRef.agent(agent.id().value().toString());
        var merchantAcc = LedgerAccountRef.merchant(merchant.id().value().toString());

        AccountProfile agentProfile = accountProfilePort.findByAccount(agentAcc)
                .orElseThrow(() -> new ForbiddenOperationException("Agent accountRef profile not found"));
        if (agentProfile.status() != Status.ACTIVE) {
            throw new ForbiddenOperationException("Agent is not active");
        }

        AccountProfile merchantProfile = accountProfilePort.findByAccount(merchantAcc)
                .orElseThrow(() -> new ForbiddenOperationException("Merchant accountRef profile not found"));
        if (merchantProfile.status() != Status.ACTIVE) {
            throw new ForbiddenOperationException("Merchant is not active");
        }

        Instant now = timeProviderPort.now();

        Money amount = Money.positive(command.amount());
        Money fee = feePolicyPort.merchantWithdrawFee(amount);
        Money commission = commissionPolicyPort.merchantWithdrawAgentCommission(fee);

        // Règle métier: commission ≤ fee
        if (commission.isGreaterThan(fee)) {
            throw new ForbiddenOperationException("Invalid commission: commission cannot exceed fee");
        }

        Money platformRevenue = fee.minus(commission);
        Money totalDebitedMerchant = amount.plus(fee);

        // --- Sufficient funds check (merchant)
        Money available = ledgerQueryPort.netBalance(merchantAcc);
        if (totalDebitedMerchant.isGreaterThan(available)) {
            throw new InsufficientFundsException(
                    "Insufficient merchant funds: need " + totalDebitedMerchant + " but available " + available
            );
        }

        Transaction tx = Transaction.merchantWithdrawAtAgent(amount, now);
        tx = transactionRepositoryPort.save(tx);

        var clearingAcc = LedgerAccountRef.platformClearing();
        var feeAcc = LedgerAccountRef.platformFeeRevenue();

        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(tx.id(), merchantAcc, totalDebitedMerchant),
                LedgerEntry.credit(tx.id(), clearingAcc, amount),
                LedgerEntry.credit(tx.id(), agentAcc, commission),
                LedgerEntry.credit(tx.id(), feeAcc, platformRevenue)
        ));

        Map<String, String> metadata = new HashMap<>();
        metadata.put("transactionId", tx.id().toString());
        metadata.put("merchantCode", command.merchantCode());
        metadata.put("agentCode", command.agentCode());

        auditPort.publish(new AuditEvent(
                "MERCHANT_WITHDRAW_AT_AGENT",
                command.actorContext().actorType().name(),
                command.actorContext().actorId(),
                now,
                metadata
        ));

        MerchantWithdrawAtAgentResult result = new MerchantWithdrawAtAgentResult(
                tx.id().toString(),
                merchant.code().value(),
                agent.code().value(),
                amount.asBigDecimal(),
                fee.asBigDecimal(),
                commission.asBigDecimal(),
                totalDebitedMerchant.asBigDecimal()
        );

        idempotencyPort.save(command.idempotencyKey(), result);
        return result;
    }
}
