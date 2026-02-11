package com.kori.bootstrap.config;

import com.kori.application.guard.OperationStatusGuards;
import com.kori.application.handler.OnAgentStatusChangedHandler;
import com.kori.application.handler.OnClientStatusChangedHandler;
import com.kori.application.handler.OnMerchantStatusChangedHandler;
import com.kori.application.port.in.*;
import com.kori.application.port.out.*;
import com.kori.application.security.LedgerAccessPolicy;
import com.kori.application.usecase.*;
import com.kori.query.port.in.*;
import com.kori.query.port.out.*;
import com.kori.query.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class ApplicationWiringConfig {

    // -----------------------------
    // Use-cases
    // -----------------------------

    @Bean
    public EnrollCardUseCase enrollCardUseCase(
            PlatformTransactionManager transactionManager,
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
            LedgerQueryPort ledgerQueryPort,
            PlatformConfigPort platformConfigPort,
            AuditPort auditPort,
            PinHasherPort pinHasherPort,
            OperationStatusGuards operationStatusGuards) {
        var useCase = new EnrollCardService(
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
                ledgerQueryPort,
                platformConfigPort,
                auditPort,
                pinHasherPort,
                operationStatusGuards
        );

        // Make this @Transactional
        var transactionTemplate = new TransactionTemplate(transactionManager);
        return command -> transactionTemplate.execute(__ -> useCase.execute(command));
    }

    @Bean
    public AddCardToExistingClientUseCase addCardToExistingClientUseCase(
            PlatformTransactionManager transactionManager,
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            IdGeneratorPort idGeneratorPort,
            ClientRepositoryPort clientRepositoryPort,
            CardRepositoryPort cardRepositoryPort,
            AgentRepositoryPort agentRepositoryPort,
            TransactionRepositoryPort transactionRepositoryPort,
            FeePolicyPort feePolicyPort,
            CommissionPolicyPort commissionPolicyPort,
            LedgerAppendPort ledgerAppendPort,
            LedgerQueryPort ledgerQueryPort,
            PlatformConfigPort platformConfigPort,
            AuditPort auditPort,
            PinHasherPort pinHasherPort,
            OperationStatusGuards operationStatusGuards
    ) {
        var useCase = new AddCardToExistingClientService(
                timeProviderPort,
                idempotencyPort,
                idGeneratorPort,
                clientRepositoryPort,
                cardRepositoryPort,
                agentRepositoryPort,
                transactionRepositoryPort,
                feePolicyPort,
                commissionPolicyPort,
                ledgerAppendPort,
                ledgerQueryPort,
                platformConfigPort,
                auditPort,
                pinHasherPort,
                operationStatusGuards
        );

        var transactionTemplate = new TransactionTemplate(transactionManager);
        return command -> transactionTemplate.execute(__ -> useCase.execute(command));
    }

    @Bean
    public PayByCardUseCase payByCardUseCase(
            PlatformTransactionManager transactionManager,
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            IdGeneratorPort idGeneratorPort,
            TerminalRepositoryPort terminalRepositoryPort,
            MerchantRepositoryPort merchantRepositoryPort,
            ClientRepositoryPort clientRepositoryPort,
            CardRepositoryPort cardRepositoryPort,
            TransactionRepositoryPort transactionRepositoryPort,
            FeePolicyPort feePolicyPort,
            CardSecurityPolicyPort cardSecurityPolicyPort,
            LedgerAppendPort ledgerAppendPort,
            LedgerQueryPort ledgerQueryPort,
            LedgerAccountLockPort ledgerAccountLockPort,
            AuditPort auditPort,
            PinHasherPort pinHasherPort,
            OperationStatusGuards operationStatusGuards) {
        var useCase = new PayByCardService(
                timeProviderPort,
                idempotencyPort,
                idGeneratorPort,
                terminalRepositoryPort,
                merchantRepositoryPort,
                clientRepositoryPort,
                cardRepositoryPort,
                transactionRepositoryPort,
                feePolicyPort,
                cardSecurityPolicyPort,
                ledgerAppendPort,
                ledgerQueryPort,
                ledgerAccountLockPort,
                auditPort,
                pinHasherPort,
                operationStatusGuards
        );

        // Make this @Transactional
        var transactionTemplate = new TransactionTemplate(transactionManager);
        return command -> transactionTemplate.execute(__ -> useCase.execute(command));
    }

    @Bean
    public MerchantWithdrawAtAgentUseCase merchantWithdrawAtAgentUseCase(
            PlatformTransactionManager transactionManager,
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            IdGeneratorPort idGeneratorPort,
            MerchantRepositoryPort merchantRepositoryPort,
            AgentRepositoryPort agentRepositoryPort,
            FeePolicyPort feePolicyPort,
            CommissionPolicyPort commissionPolicyPort,
            LedgerQueryPort ledgerQueryPort,
            LedgerAccountLockPort ledgerAccountLockPort,
            PlatformConfigPort platformConfigPort,
            TransactionRepositoryPort transactionRepositoryPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort,
            OperationStatusGuards operationStatusGuards
    ) {
        var useCase = new MerchantWithdrawAtAgentService(
                timeProviderPort,
                idempotencyPort,
                idGeneratorPort,
                merchantRepositoryPort,
                agentRepositoryPort,
                feePolicyPort,
                commissionPolicyPort,
                ledgerQueryPort,
                ledgerAccountLockPort,
                platformConfigPort,
                transactionRepositoryPort,
                ledgerAppendPort,
                auditPort,
                operationStatusGuards
        );

        // Make this @Transactional
        var transactionTemplate = new TransactionTemplate(transactionManager);
        return command -> transactionTemplate.execute(__ -> useCase.execute(command));
    }

    @Bean
    public CashInByAgentUseCase cashInByAgentUseCase(
            PlatformTransactionManager transactionManager,
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            IdGeneratorPort idGeneratorPort,
            AgentRepositoryPort agentRepositoryPort,
            ClientRepositoryPort clientRepositoryPort,
            LedgerQueryPort ledgerQueryPort,
            PlatformConfigPort platformConfigPort,
            TransactionRepositoryPort transactionRepositoryPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort,
            OperationStatusGuards operationStatusGuards
    ) {
        var useCase = new CashInByAgentService(
                timeProviderPort,
                idempotencyPort,
                idGeneratorPort,
                agentRepositoryPort,
                clientRepositoryPort,
                ledgerQueryPort,
                platformConfigPort,
                transactionRepositoryPort,
                ledgerAppendPort,
                auditPort,
                operationStatusGuards
        );

        var transactionTemplate = new TransactionTemplate(transactionManager);
        return command -> transactionTemplate.execute(__ -> useCase.execute(command));
    }

    @Bean
    public AgentBankDepositReceiptUseCase agentBankDepositReceiptUseCase(
            PlatformTransactionManager transactionManager,
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            IdGeneratorPort idGeneratorPort,
            AgentRepositoryPort agentRepositoryPort,
            TransactionRepositoryPort transactionRepositoryPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort
    ) {
        var useCase = new AgentBankDepositReceiptService(
                timeProviderPort,
                idempotencyPort,
                idGeneratorPort,
                agentRepositoryPort,
                transactionRepositoryPort,
                ledgerAppendPort,
                auditPort
        );

        var transactionTemplate = new TransactionTemplate(transactionManager);
        return command -> transactionTemplate.execute(__ -> useCase.execute(command));
    }

    @Bean
    public RequestAgentPayoutUseCase agentPayoutUseCase(
            PlatformTransactionManager transactionManager,
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            AgentRepositoryPort agentRepositoryPort,
            LedgerAppendPort ledgerAppendPort,
            LedgerQueryPort ledgerQueryPort,
            LedgerAccountLockPort ledgerAccountLockPort,
            TransactionRepositoryPort transactionRepositoryPort,
            PayoutRepositoryPort payoutRepositoryPort,
            AuditPort auditPort,
            IdGeneratorPort idGeneratorPort
    ) {
        var useCase = new RequestAgentPayoutService(
                timeProviderPort,
                idempotencyPort,
                agentRepositoryPort,
                ledgerAppendPort,
                ledgerQueryPort,
                ledgerAccountLockPort,
                transactionRepositoryPort,
                payoutRepositoryPort,
                auditPort,
                idGeneratorPort
        );

        var transactionTemplate = new TransactionTemplate(transactionManager);
        return command -> transactionTemplate.execute(__ -> useCase.execute(command));
    }

    @Bean
    public CompleteAgentPayoutUseCase completeAgentPayoutUseCase(
            PlatformTransactionManager transactionManager,
            TimeProviderPort timeProviderPort,
            PayoutRepositoryPort payoutRepositoryPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort) {
        var useCase = new CompleteAgentPayoutService(
                timeProviderPort,
                payoutRepositoryPort,
                ledgerAppendPort,
                auditPort
        );

        var transactionTemplate = new TransactionTemplate(transactionManager);
        return command -> transactionTemplate.execute(__ -> useCase.execute(command));
    }

    @Bean
    public FailAgentPayoutUseCase failAgentPayoutUseCase(
            PlatformTransactionManager transactionManager,
            TimeProviderPort timeProviderPort,
            PayoutRepositoryPort payoutRepositoryPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort) {
        var useCase = new FailAgentPayoutService(
                timeProviderPort,
                payoutRepositoryPort,
                ledgerAppendPort,
                auditPort
        );

        var transactionTemplate = new TransactionTemplate(transactionManager);
        return command -> transactionTemplate.execute(__ -> useCase.execute(command));
    }

    @Bean
    public RequestClientRefundUseCase requestClientRefundUseCase(
            PlatformTransactionManager transactionManager,
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            ClientRepositoryPort clientRepositoryPort,
            LedgerAppendPort ledgerAppendPort,
            LedgerQueryPort ledgerQueryPort,
            LedgerAccountLockPort ledgerAccountLockPort,
            TransactionRepositoryPort transactionRepositoryPort,
            ClientRefundRepositoryPort clientRefundRepositoryPort,
            AuditPort auditPort,
            IdGeneratorPort idGeneratorPort
    ) {
        var useCase = new RequestClientRefundService(
                timeProviderPort,
                idempotencyPort,
                clientRepositoryPort,
                ledgerAppendPort,
                ledgerQueryPort,
                ledgerAccountLockPort,
                transactionRepositoryPort,
                clientRefundRepositoryPort,
                auditPort,
                idGeneratorPort
        );

        var transactionTemplate = new TransactionTemplate(transactionManager);
        return command -> transactionTemplate.execute(__ -> useCase.execute(command));
    }

    @Bean
    public CompleteClientRefundUseCase completeClientRefundUseCase(
            PlatformTransactionManager transactionManager,
            TimeProviderPort timeProviderPort,
            ClientRefundRepositoryPort clientRefundRepositoryPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort) {
        var useCase = new CompleteClientRefundService(
                timeProviderPort,
                clientRefundRepositoryPort,
                ledgerAppendPort,
                auditPort
        );

        var transactionTemplate = new TransactionTemplate(transactionManager);
        return command -> transactionTemplate.execute(__ -> useCase.execute(command));
    }

    @Bean
    public FailClientRefundUseCase failClientRefundUseCase(
            PlatformTransactionManager transactionManager,
            TimeProviderPort timeProviderPort,
            ClientRefundRepositoryPort clientRefundRepositoryPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort) {
        var useCase = new FailClientRefundService(
                timeProviderPort,
                clientRefundRepositoryPort,
                ledgerAppendPort,
                auditPort
        );

        var transactionTemplate = new TransactionTemplate(transactionManager);
        return command -> transactionTemplate.execute(__ -> useCase.execute(command));
    }

    @Bean
    public ReversalUseCase reversalUseCase(
            PlatformTransactionManager transactionManager,
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            TransactionRepositoryPort transactionRepositoryPort,
            LedgerQueryPort ledgerQueryPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort,
            IdGeneratorPort idGeneratorPort,
            FeeConfigPort feeConfigPort,
            PlatformConfigPort platformConfigPort
    ) {
        var useCase = new ReversalService(
                timeProviderPort,
                idempotencyPort,
                transactionRepositoryPort,
                ledgerQueryPort,
                ledgerAppendPort,
                auditPort,
                idGeneratorPort,
                feeConfigPort,
                platformConfigPort
        );

        //Make this @Transactional
        var transactionTemplate = new TransactionTemplate(transactionManager);
        return command -> transactionTemplate.execute(__ -> useCase.execute(command));
    }

    @Bean
    public CreateAgentUseCase createAgentUseCase(
            AgentRepositoryPort agentRepositoryPort,
            AccountProfilePort accountProfilePort,
            IdempotencyPort idempotencyPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort,
            CodeGeneratorPort codeGeneratorPort,
            IdGeneratorPort idGeneratorPort
    ) {
        return new CreateAgentService(
                agentRepositoryPort,
                accountProfilePort,
                idempotencyPort,
                auditPort,
                timeProviderPort,
                codeGeneratorPort,
                idGeneratorPort
        );
    }

    @Bean
    public CreateMerchantUseCase createMerchantUseCase(
            MerchantRepositoryPort merchantRepositoryPort,
            AccountProfilePort accountProfilePort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            CodeGeneratorPort codeGeneratorPort,
            IdGeneratorPort idGeneratorPort
    ) {
        return new CreateMerchantService(
                merchantRepositoryPort,
                accountProfilePort,
                auditPort,
                timeProviderPort,
                idempotencyPort,
                codeGeneratorPort,
                idGeneratorPort
        );
    }

    @Bean
    public CreateTerminalUseCase createTerminalUseCase(
            TerminalRepositoryPort terminalRepositoryPort,
            MerchantRepositoryPort merchantRepositoryPort,
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            IdGeneratorPort idGeneratorPort,
            AuditPort auditPort
    ) {
        return new CreateTerminalService(
                terminalRepositoryPort,
                merchantRepositoryPort,
                idempotencyPort,
                timeProviderPort,
                idGeneratorPort,
                auditPort
        );
    }

    @Bean
    public CreateAdminUseCase createAdminUseCase(
            AdminRepositoryPort adminRepositoryPort,
            IdempotencyPort idempotencyPort,
            TimeProviderPort timeProviderPort,
            IdGeneratorPort idGeneratorPort,
            AuditPort auditPort
    ) {
        return new CreateAdminService(
                adminRepositoryPort,
                idempotencyPort,
                timeProviderPort,
                idGeneratorPort,
                auditPort
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
    public UpdateAgentStatusUseCase updateAgentStatusUseCase(
            AgentRepositoryPort agentRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort,
            DomainEventPublisherPort domainEventPublisherPort,
            LedgerQueryPort ledgerQueryPort
    ) {
        return new UpdateAgentStatusService(
                agentRepositoryPort,
                auditPort,
                timeProviderPort,
                domainEventPublisherPort,
                ledgerQueryPort
        ) {
        };
    }

    @Bean
    public UpdateAccountProfileStatusUseCase updateAccountProfileStatusUseCase(
            AccountProfilePort accountProfilePort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort,
            DomainEventPublisherPort domainEventPublisherPort
    ) {
        return new UpdateAccountProfileStatusService(
                accountProfilePort,
                auditPort,
                timeProviderPort,
                domainEventPublisherPort
        );
    }

    @Bean
    public UpdateClientStatusUseCase updateClientStatusUseCase(
            ClientRepositoryPort clientRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort,
            DomainEventPublisherPort domainEventPublisherPort,
            LedgerQueryPort ledgerQueryPort
    ) {
        return new UpdateClientStatusService(
                clientRepositoryPort,
                auditPort,
                timeProviderPort,
                domainEventPublisherPort,
                ledgerQueryPort
        );
    }

    @Bean
    public UpdateMerchantStatusUseCase updateMerchantStatusUseCase(
            MerchantRepositoryPort merchantRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort,
            DomainEventPublisherPort domainEventPublisherPort,
            LedgerQueryPort ledgerQueryPort
    ) {
        return new UpdateMerchantStatusService(
                merchantRepositoryPort,
                auditPort,
                timeProviderPort,
                domainEventPublisherPort,
                ledgerQueryPort
        );
    }

    @Bean
    public UpdateTerminalStatusUseCase updateTerminalStatusUseCase(
            TerminalRepositoryPort terminalRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort
    ) {
        return new UpdateTerminalStatusService(
                terminalRepositoryPort,
                auditPort,
                timeProviderPort
        );
    }

    @Bean
    public UpdateAdminStatusUseCase updateAdminStatusUseCase(
            AdminRepositoryPort adminRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort
    ) {
        return new UpdateAdminStatusService(
                adminRepositoryPort,
                auditPort,
                timeProviderPort
        );
    }

    @Bean
    public UpdateFeeConfigUseCase updateFeeConfigUseCase(
            FeeConfigPort feeConfigPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort
    ) {
        return new UpdateFeeConfigService(
                feeConfigPort,
                auditPort,
                timeProviderPort
        );
    }

    @Bean
    public UpdateCommissionConfigUseCase updateCommissionConfigUseCase(
            CommissionConfigPort commissionConfigPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort
    ) {
        return new UpdateCommissionConfigService(
                commissionConfigPort,
                auditPort,
                timeProviderPort
        );
    }

    // -----------------------------
    // Policy and Guard
    // -----------------------------

    @Bean
    public LedgerAccessPolicy ledgerAccessPolicy() {
        return new LedgerAccessPolicy();
    }

    @Bean
    public OperationStatusGuards operationStatusGuards(AccountProfilePort accountProfilePort) {
        return new OperationStatusGuards(accountProfilePort);
    }

    // -----------------------------
    // Read-side (Phase 1 services reused)
    // -----------------------------

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

    @Bean
    public BackofficeTransactionQueryUseCase backofficeTransactionQueryUseCase(
            BackofficeTransactionReadPort readPort
    ) {
        return new BackofficeTransactionQueryService(readPort);
    }

    @Bean
    public BackofficeAuditEventQueryUseCase backofficeAuditEventQueryUseCase(
            BackofficeAuditEventReadPort readPort
    ) {
        return new BackofficeAuditEventQueryService(readPort);
    }

    @Bean
    public BackofficeActorQueryUseCase backofficeActorQueryUseCase(
            BackofficeActorReadPort readPort
    ) {
        return new BackofficeActorQueryService(readPort);
    }

    @Bean
    public BackofficeActorDetailQueryUseCase backofficeActorDetailQueryUseCase(
            BackofficeActorDetailReadPort readPort
    ) {
        return new BackofficeActorDetailQueryService(readPort);
    }

    @Bean
    public BackofficeLookupQueryUseCase backofficeLookupQueryUseCase(
            BackofficeLookupReadPort readPort
    ) {
        return new BackofficeLookupQueryService(readPort);
    }

    // -----------------------------
    // Handlers
    // -----------------------------

    @Bean
    public OnClientStatusChangedHandler onClientStatusChangedHandler(
            AccountProfilePort accountProfilePort,
            CardRepositoryPort cardRepositoryPort
    ) {
        return new OnClientStatusChangedHandler(accountProfilePort, cardRepositoryPort);
    }

    @Bean
    public OnMerchantStatusChangedHandler onMerchantStatusChangedHandler(
            AccountProfilePort accountProfilePort,
            TerminalRepositoryPort terminalRepositoryPort
    ) {
        return new OnMerchantStatusChangedHandler(accountProfilePort, terminalRepositoryPort);
    }

    @Bean
    public OnAgentStatusChangedHandler onAgentStatusChangedHandler(
            AccountProfilePort accountProfilePort
    ) {
        return new OnAgentStatusChangedHandler(accountProfilePort);
    }

}
