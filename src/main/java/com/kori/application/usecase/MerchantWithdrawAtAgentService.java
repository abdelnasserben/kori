package com.kori.application.usecase;

import com.kori.application.command.MerchantWithdrawAtAgentCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.InsufficientFundsException;
import com.kori.application.guard.ActorGuards;
import com.kori.application.guard.OperationStatusGuards;
import com.kori.application.guard.PricingGuards;
import com.kori.application.port.in.MerchantWithdrawAtAgentUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.MerchantWithdrawAtAgentResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.merchant.MerchantCode;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MerchantWithdrawAtAgentService implements MerchantWithdrawAtAgentUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;
    private final IdGeneratorPort idGeneratorPort;

    private final MerchantRepositoryPort merchantRepositoryPort;
    private final AgentRepositoryPort agentRepositoryPort;

    private final FeePolicyPort feePolicyPort;
    private final CommissionPolicyPort commissionPolicyPort;

    private final LedgerQueryPort ledgerQueryPort;

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final LedgerAppendPort ledgerAppendPort;
    private final AuditPort auditPort;

    private final OperationStatusGuards operationStatusGuards;

    public MerchantWithdrawAtAgentService(TimeProviderPort timeProviderPort,
                                          IdempotencyPort idempotencyPort,
                                          IdGeneratorPort idGeneratorPort,
                                          MerchantRepositoryPort merchantRepositoryPort,
                                          AgentRepositoryPort agentRepositoryPort,
                                          FeePolicyPort feePolicyPort,
                                          CommissionPolicyPort commissionPolicyPort,
                                          LedgerQueryPort ledgerQueryPort,
                                          TransactionRepositoryPort transactionRepositoryPort,
                                          LedgerAppendPort ledgerAppendPort,
                                          AuditPort auditPort,
                                          OperationStatusGuards operationStatusGuards) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.idGeneratorPort = idGeneratorPort;
        this.merchantRepositoryPort = merchantRepositoryPort;
        this.agentRepositoryPort = agentRepositoryPort;
        this.feePolicyPort = feePolicyPort;
        this.commissionPolicyPort = commissionPolicyPort;
        this.ledgerQueryPort = ledgerQueryPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.auditPort = auditPort;
        this.operationStatusGuards = operationStatusGuards;
    }

    @Override
    @Transactional
    public MerchantWithdrawAtAgentResult execute(MerchantWithdrawAtAgentCommand command) {
        var cached = idempotencyPort.find(command.idempotencyKey(), command.idempotencyRequestHash(), MerchantWithdrawAtAgentResult.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        // Initié par AGENT uniquement
        ActorGuards.requireAgent(command.actorContext(), "initiate MerchantWithdrawAtAgent");

        // Resolve AGENT by code
        Agent agent = agentRepositoryPort.findByCode(AgentCode.of(command.agentCode()))
                .orElseThrow(() -> new ForbiddenOperationException("Agent not found"));

        operationStatusGuards.requireActiveAgent(agent);

        // Resolve MERCHANT by code
        Merchant merchant = merchantRepositoryPort.findByCode(MerchantCode.of(command.merchantCode()))
                .orElseThrow(() -> new ForbiddenOperationException("Merchant not found"));

        operationStatusGuards.requireActiveMerchant(merchant);

        var agentAcc = LedgerAccountRef.agent(agent.id().value().toString());
        var merchantAcc = LedgerAccountRef.merchant(merchant.id().value().toString());

        Instant now = timeProviderPort.now();

        Money amount = Money.positive(command.amount());
        Money fee = feePolicyPort.merchantWithdrawFee(amount);
        Money commission = commissionPolicyPort.merchantWithdrawAgentCommission(fee);

        var breakdown = PricingGuards.feeMinusCommission(fee, commission, "MERCHANT_WITHDRAW");
        Money platformRevenue = breakdown.platformRevenue();
        Money totalDebitedMerchant = amount.plus(fee);

        // --- Sufficient funds check (merchant)
        Money available = ledgerQueryPort.netBalance(merchantAcc);
        if (totalDebitedMerchant.isGreaterThan(available)) {
            throw new InsufficientFundsException(
                    "Insufficient merchant funds: need " + totalDebitedMerchant + " but available " + available
            );
        }

        TransactionId txId = new TransactionId(idGeneratorPort.newUuid());
        Transaction tx = Transaction.merchantWithdrawAtAgent(txId, amount, now);
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
        metadata.put("transactionId", tx.id().value().toString());
        metadata.put("merchantCode", command.merchantCode());
        metadata.put("agentCode", command.agentCode());

        auditPort.publish(AuditBuilder.buildBasicAudit(
                "MERCHANT_WITHDRAW_AT_AGENT",
                command.actorContext(),
                now,
                metadata
        ));

        MerchantWithdrawAtAgentResult result = new MerchantWithdrawAtAgentResult(
                tx.id().value().toString(),
                merchant.code().value(),
                agent.code().value(),
                amount.asBigDecimal(),
                fee.asBigDecimal(),
                commission.asBigDecimal(),
                totalDebitedMerchant.asBigDecimal()
        );

        idempotencyPort.save(command.idempotencyKey(), command.idempotencyRequestHash(), result);
        return result;
    }
}
