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
    public EnrollCardUseCase enrollCardUseCase(TimeProviderPort timeProviderPort,
                                               IdempotencyPort idempotencyPort,
                                               ClientRepositoryPort clientRepositoryPort,
                                               AccountRepositoryPort accountRepositoryPort,
                                               CardRepositoryPort cardRepositoryPort,
                                               AgentRepositoryPort agentRepositoryPort,
                                               TransactionRepositoryPort transactionRepositoryPort,
                                               FeePolicyPort feePolicyPort,
                                               CommissionPolicyPort commissionPolicyPort,
                                               LedgerAppendPort ledgerAppendPort,
                                               AuditPort auditPort) {
        return new EnrollCardService(
                timeProviderPort,
                idempotencyPort,
                clientRepositoryPort,
                accountRepositoryPort,
                cardRepositoryPort,
                agentRepositoryPort,
                transactionRepositoryPort,
                feePolicyPort,
                commissionPolicyPort,
                ledgerAppendPort,
                auditPort
        );
    }

    @Bean
    public PayByCardUseCase payByCardUseCase(TimeProviderPort timeProviderPort,
                                             IdempotencyPort idempotencyPort,
                                             TerminalRepositoryPort terminalRepositoryPort,
                                             MerchantRepositoryPort merchantRepositoryPort,
                                             CardRepositoryPort cardRepositoryPort,
                                             AccountRepositoryPort accountRepositoryPort,
                                             TransactionRepositoryPort transactionRepositoryPort,
                                             FeePolicyPort feePolicyPort,
                                             CardSecurityPolicyPort cardSecurityPolicyPort,
                                             LedgerAppendPort ledgerAppendPort,
                                             AuditPort auditPort) {
        return new PayByCardService(
                timeProviderPort,
                idempotencyPort,
                terminalRepositoryPort,
                merchantRepositoryPort,
                cardRepositoryPort,
                accountRepositoryPort,
                transactionRepositoryPort,
                feePolicyPort,
                cardSecurityPolicyPort,
                ledgerAppendPort,
                auditPort
        );
    }

    @Bean
    public MerchantWithdrawAtAgentUseCase merchantWithdrawAtAgentUseCase(
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            MerchantRepositoryPort merchantRepositoryPort,
            AgentRepositoryPort agentRepositoryPort,
            FeePolicyPort feePolicyPort,
            CommissionPolicyPort commissionPolicyPort,
            TransactionRepositoryPort transactionRepositoryPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort
    ) {
        return new MerchantWithdrawAtAgentService(
                timeProviderPort,
                idempotencyPort,
                merchantRepositoryPort,
                agentRepositoryPort,
                feePolicyPort,
                commissionPolicyPort,
                transactionRepositoryPort,
                ledgerAppendPort,
                auditPort
        );
    }

    @Bean
    public AgentPayoutUseCase agentPayoutUseCase(
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            AgentRepositoryPort agentRepositoryPort,
            LedgerQueryPort ledgerQueryPort,
            LedgerAppendPort ledgerAppendPort,
            TransactionRepositoryPort transactionRepositoryPort,
            PayoutRepositoryPort payoutRepositoryPort,
            AuditPort auditPort
    ) {
        return new AgentPayoutService(
                timeProviderPort,
                idempotencyPort,
                agentRepositoryPort,
                ledgerQueryPort,
                ledgerAppendPort,
                transactionRepositoryPort,
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
            AuditPort auditPort
    ) {
        return new ReversalService(
                timeProviderPort,
                idempotencyPort,
                transactionRepositoryPort,
                ledgerQueryPort,
                ledgerAppendPort,
                auditPort
        );
    }

    @Bean
    public AgentUpdateCardStatusUseCase agentUpdateCardStatusUseCase(
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            AgentRepositoryPort agentRepositoryPort,
            CardRepositoryPort cardRepositoryPort,
            AuditPort auditPort
    ) {
        return new AgentUpdateCardStatusService(
                timeProviderPort,
                idempotencyPort,
                agentRepositoryPort,
                cardRepositoryPort,
                auditPort
        );
    }

    @Bean
    public AdminUnblockCardUseCase adminUnblockCardUseCase(
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            CardRepositoryPort cardRepositoryPort,
            AuditPort auditPort
    ) {
        return new AdminUnblockCardService(
                timeProviderPort,
                idempotencyPort,
                cardRepositoryPort,
                auditPort
        );
    }

    @Bean
    public AdminUpdateCardStatusUseCase adminUpdateCardStatusUseCase(
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            CardRepositoryPort cardRepositoryPort,
            AuditPort auditPort
    ) {
        return new AdminUpdateCardStatusService(
                timeProviderPort,
                idempotencyPort,
                cardRepositoryPort,
                auditPort
        );
    }

    @Bean
    public AdminUpdateAccountStatusUseCase adminUpdateAccountStatusUseCase(
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            AccountRepositoryPort accountRepositoryPort,
            AuditPort auditPort
    ) {
        return new AdminUpdateAccountStatusService(
                timeProviderPort,
                idempotencyPort,
                accountRepositoryPort,
                auditPort
        );
    }

    @Bean
    public AdminUpdateClientStatusUseCase adminUpdateClientStatusUseCase(
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            ClientRepositoryPort clientRepositoryPort,
            AuditPort auditPort
    ) {
        return new AdminUpdateClientStatusService(
                timeProviderPort,
                idempotencyPort,
                clientRepositoryPort,
                auditPort
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
