package com.kori.bootstrap.config;

import com.kori.application.port.in.*;
import com.kori.application.port.out.*;
import com.kori.application.security.LedgerAccessPolicy;
import com.kori.application.usecase.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationWiringConfig {

    // -----------------------------
    // Use-cases
    // -----------------------------

    @Bean
    public EnrollCardUseCase enrollCardUseCase(
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            IdGeneratorPort idGeneratorPort,
            ClientRepositoryPort clientRepositoryPort,
            CardRepositoryPort cardRepositoryPort,
            AgentRepositoryPort agentRepositoryPort,
            TransactionRepositoryPort transactionRepositoryPort,
            AccountProfilePort accountProfilePort,
            FeePolicyPort feePolicyPort,
            CommissionPolicyPort commissionPolicyPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort,
            PinHasherPort pinHasherPort) {
        return new EnrollCardService(
                timeProviderPort,
                idempotencyPort,
                idGeneratorPort,
                clientRepositoryPort,
                cardRepositoryPort,
                agentRepositoryPort,
                transactionRepositoryPort,
                accountProfilePort,
                feePolicyPort,
                commissionPolicyPort,
                ledgerAppendPort,
                auditPort,
                pinHasherPort
        );
    }

    @Bean
    public PayByCardUseCase payByCardUseCase(
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            IdGeneratorPort idGeneratorPort,
            TerminalRepositoryPort terminalRepositoryPort,
            MerchantRepositoryPort merchantRepositoryPort,
            CardRepositoryPort cardRepositoryPort,
            TransactionRepositoryPort transactionRepositoryPort,
            FeePolicyPort feePolicyPort,
            CardSecurityPolicyPort cardSecurityPolicyPort,
            LedgerAppendPort ledgerAppendPort,
            LedgerQueryPort ledgerQueryPort,
            AccountProfilePort accountProfilePort,
            AuditPort auditPort,
            PinHasherPort pinHasherPort) {
        return new PayByCardService(
                timeProviderPort,
                idempotencyPort,
                idGeneratorPort,
                terminalRepositoryPort,
                merchantRepositoryPort,
                cardRepositoryPort,
                transactionRepositoryPort,
                feePolicyPort,
                cardSecurityPolicyPort,
                ledgerAppendPort,
                ledgerQueryPort,
                accountProfilePort,
                auditPort,
                pinHasherPort
        );
    }

    @Bean
    public MerchantWithdrawAtAgentUseCase merchantWithdrawAtAgentUseCase(
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            IdGeneratorPort idGeneratorPort,
            MerchantRepositoryPort merchantRepositoryPort,
            AgentRepositoryPort agentRepositoryPort,
            AccountProfilePort accountProfilePort,
            FeePolicyPort feePolicyPort,
            CommissionPolicyPort commissionPolicyPort,
            LedgerQueryPort ledgerQueryPort,
            TransactionRepositoryPort transactionRepositoryPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort
    ) {
        return new MerchantWithdrawAtAgentService(
                timeProviderPort,
                idempotencyPort,
                idGeneratorPort,
                merchantRepositoryPort,
                agentRepositoryPort,
                accountProfilePort,
                feePolicyPort,
                commissionPolicyPort,
                ledgerQueryPort,
                transactionRepositoryPort,
                ledgerAppendPort,
                auditPort
        );
    }

    @Bean
    public RequestAgentPayoutUseCase agentPayoutUseCase(
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            AgentRepositoryPort agentRepositoryPort,
            LedgerQueryPort ledgerQueryPort,
            TransactionRepositoryPort transactionRepositoryPort,
            PayoutRepositoryPort payoutRepositoryPort,
            AuditPort auditPort,
            IdGeneratorPort idGeneratorPort
    ) {
        return new RequestAgentPayoutService(
                timeProviderPort,
                idempotencyPort,
                agentRepositoryPort,
                ledgerQueryPort,
                transactionRepositoryPort,
                payoutRepositoryPort,
                auditPort,
                idGeneratorPort
        );
    }

    @Bean
    public CompleteAgentPayoutUseCase completeAgentPayoutUseCase(
            TimeProviderPort timeProviderPort,
            PayoutRepositoryPort payoutRepositoryPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort) {
        return new CompleteAgentPayoutService(
                timeProviderPort,
                payoutRepositoryPort,
                ledgerAppendPort,
                auditPort
        );
    }

    @Bean
    public FailAgentPayoutUseCase failAgentPayoutUseCase(
            TimeProviderPort timeProviderPort,
            PayoutRepositoryPort payoutRepositoryPort,
            AuditPort auditPort) {
        return new FailAgentPayoutService(
                timeProviderPort,
                payoutRepositoryPort,
                auditPort
        );
    }

    @Bean
    public ReversalUseCase reversalUseCase(
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            TransactionRepositoryPort transactionRepositoryPort,
            LedgerQueryPort ledgerQueryPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort,
            IdGeneratorPort idGeneratorPort
    ) {
        return new ReversalService(
                timeProviderPort,
                idempotencyPort,
                transactionRepositoryPort,
                ledgerQueryPort,
                ledgerAppendPort,
                auditPort,
                idGeneratorPort
        );
    }

    @Bean
    public AgentUpdateCardStatusUseCase agentUpdateCardStatusUseCase(
            TimeProviderPort timeProviderPort,
            AgentRepositoryPort agentRepositoryPort,
            CardRepositoryPort cardRepositoryPort,
            AuditPort auditPort
    ) {
        return new AgentUpdateCardStatusService(
                timeProviderPort,
                agentRepositoryPort,
                cardRepositoryPort,
                auditPort
        );
    }

    @Bean
    public AdminUnblockCardUseCase adminUnblockCardUseCase(
            TimeProviderPort timeProviderPort,
            CardRepositoryPort cardRepositoryPort,
            AuditPort auditPort
    ) {
        return new AdminUnblockCardService(
                timeProviderPort,
                cardRepositoryPort,
                auditPort
        );
    }

    @Bean
    public AdminUpdateCardStatusUseCase adminUpdateCardStatusUseCase(
            TimeProviderPort timeProviderPort,
            CardRepositoryPort cardRepositoryPort,
            AuditPort auditPort
    ) {
        return new AdminUpdateCardStatusService(
                timeProviderPort,
                cardRepositoryPort,
                auditPort
        );
    }


    @Bean
    public UpdateAccountProfileStatusUseCase updateAccountProfileStatusUseCase(
            AccountProfilePort accountProfilePort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort
    ) {
        return new UpdateAccountProfileStatusService(
                accountProfilePort,
                auditPort,
                timeProviderPort
        );
    }

    @Bean
    public UpdateClientStatusUseCase updateClientStatusUseCase(
            ClientRepositoryPort clientRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort
    ) {
        return new UpdateClientStatusService(
                clientRepositoryPort,
                auditPort,
                timeProviderPort
        );
    }

    // -----------------------------
    // Read-side (Phase 1 services reused)
    // -----------------------------

    @Bean
    public LedgerAccessPolicy ledgerAccessPolicy() {
        return new LedgerAccessPolicy();
    }

    @Bean
    public GetBalanceUseCase getBalanceUseCase(
            LedgerQueryPort ledgerQueryPort,
            LedgerAccessPolicy ledgerAccessPolicy
    ) {
        return new GetBalanceService(ledgerQueryPort, ledgerAccessPolicy);
    }

    @Bean
    public SearchTransactionHistoryUseCase searchTransactionHistoryUseCase(
            LedgerQueryPort ledgerQueryPort,
            TransactionRepositoryPort transactionRepositoryPort,
            LedgerAccessPolicy ledgerAccessPolicy
    ) {
        return new SearchTransactionHistoryService(
                ledgerQueryPort,
                transactionRepositoryPort,
                ledgerAccessPolicy
        );
    }

}
