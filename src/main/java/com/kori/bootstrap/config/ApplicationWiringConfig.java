package com.kori.bootstrap.config;

import com.kori.application.handler.OnAgentStatusChangedHandler;
import com.kori.application.handler.OnClientStatusChangedHandler;
import com.kori.application.handler.OnMerchantStatusChangedHandler;
import com.kori.application.port.in.*;
import com.kori.application.port.out.*;
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
    // Other services
    // -----------------------------

    @Bean
    public AdminAccessService adminAccessService(AdminRepositoryPort adminRepositoryPort) {
        return new AdminAccessService(adminRepositoryPort);
    }

    @Bean
    public LedgerOwnerRefResolver ledgerOwnerRefResolver(
            ClientRepositoryPort clientRepositoryPort,
            MerchantRepositoryPort merchantRepositoryPort,
            AgentRepositoryPort agentRepositoryPort) {
        return new LedgerOwnerRefResolver(
                clientRepositoryPort,
                merchantRepositoryPort,
                agentRepositoryPort);
    }


    // -----------------------------
    // Use-cases
    // -----------------------------

    @Bean
    public EnrollCardUseCase enrollCardUseCase(
            PlatformTransactionManager transactionManager,
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            IdGeneratorPort idGeneratorPort,
            CodeGeneratorPort codeGeneratorPort,
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
            OperationAuthorizationService operationAuthorizationService) {
        var useCase = new EnrollCardService(
                timeProviderPort,
                idempotencyPort,
                idGeneratorPort,
                codeGeneratorPort,
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
                operationAuthorizationService
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
            OperationAuthorizationService operationAuthorizationService
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
                operationAuthorizationService
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
            OperationAuthorizationService operationAuthorizationService) {
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
                operationAuthorizationService
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
            OperationAuthorizationService operationAuthorizationService
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
                operationAuthorizationService
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
            OperationAuthorizationService operationAuthorizationService
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
                operationAuthorizationService
        );

        var transactionTemplate = new TransactionTemplate(transactionManager);
        return command -> transactionTemplate.execute(__ -> useCase.execute(command));
    }

    @Bean
    public ClientTransferUseCase clientTransferUseCase(
            PlatformTransactionManager transactionManager,
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            IdGeneratorPort idGeneratorPort,
            ClientRepositoryPort clientRepositoryPort,
            TransactionRepositoryPort transactionRepositoryPort,
            FeePolicyPort feePolicyPort,
            PlatformConfigPort platformConfigPort,
            LedgerAppendPort ledgerAppendPort,
            LedgerQueryPort ledgerQueryPort,
            LedgerAccountLockPort ledgerAccountLockPort,
            AuditPort auditPort,
            OperationAuthorizationService operationAuthorizationService
    ) {
        var useCase = new ClientTransferService(
                timeProviderPort,
                idempotencyPort,
                idGeneratorPort,
                clientRepositoryPort,
                transactionRepositoryPort,
                feePolicyPort,
                platformConfigPort,
                ledgerAppendPort,
                ledgerQueryPort,
                ledgerAccountLockPort,
                auditPort,
                operationAuthorizationService
        );

        var transactionTemplate = new TransactionTemplate(transactionManager);
        return command -> transactionTemplate.execute(__ -> useCase.execute(command));
    }

    @Bean
    public AdminReceiptAgentBankDepositUseCase agentBankDepositReceiptUseCase(
            PlatformTransactionManager transactionManager,
            AdminAccessService adminAccessService,
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            IdGeneratorPort idGeneratorPort,
            AgentRepositoryPort agentRepositoryPort,
            TransactionRepositoryPort transactionRepositoryPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort
    ) {
        var useCase = new AdminReceiptAdminServiceAgentBankDeposit(
                adminAccessService,
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
            AdminAccessService adminAccessService,
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
                adminAccessService,
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
            AdminAccessService adminAccessService,
            TimeProviderPort timeProviderPort,
            PayoutRepositoryPort payoutRepositoryPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort) {
        var useCase = new CompleteAgentPayoutService(
                adminAccessService,
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
            AdminAccessService adminAccessService,
            TimeProviderPort timeProviderPort,
            PayoutRepositoryPort payoutRepositoryPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort) {
        var useCase = new FailAgentPayoutService(
                adminAccessService,
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
            AdminAccessService adminAccessService,
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
                adminAccessService,
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
            AdminAccessService adminAccessService,
            TimeProviderPort timeProviderPort,
            ClientRefundRepositoryPort clientRefundRepositoryPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort) {
        var useCase = new CompleteClientRefundService(
                adminAccessService,
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
            AdminAccessService adminAccessService,
            TimeProviderPort timeProviderPort,
            ClientRefundRepositoryPort clientRefundRepositoryPort,
            LedgerAppendPort ledgerAppendPort,
            AuditPort auditPort) {
        var useCase = new FailClientRefundService(
                adminAccessService,
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
            AdminAccessService adminAccessService,
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
                adminAccessService,
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
            AdminAccessService adminAccessService,
            AgentRepositoryPort agentRepositoryPort,
            AccountProfilePort accountProfilePort,
            IdempotencyPort idempotencyPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort,
            CodeGeneratorPort codeGeneratorPort,
            IdGeneratorPort idGeneratorPort
    ) {
        return new CreateAgentService(
                adminAccessService,
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
            AdminAccessService adminAccessService,
            MerchantRepositoryPort merchantRepositoryPort,
            AccountProfilePort accountProfilePort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            CodeGeneratorPort codeGeneratorPort,
            IdGeneratorPort idGeneratorPort
    ) {
        return new CreateMerchantService(
                adminAccessService,
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
            AdminAccessService adminAccessService,
            TerminalRepositoryPort terminalRepositoryPort,
            MerchantRepositoryPort merchantRepositoryPort,
            TimeProviderPort timeProviderPort,
            IdempotencyPort idempotencyPort,
            IdGeneratorPort idGeneratorPort,
            AuditPort auditPort
    ) {
        return new CreateTerminalService(
                adminAccessService,
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
            AdminAccessService adminAccessService,
            AdminRepositoryPort adminRepositoryPort,
            IdempotencyPort idempotencyPort,
            TimeProviderPort timeProviderPort,
            IdGeneratorPort idGeneratorPort,
            AuditPort auditPort
    ) {
        return new CreateAdminService(
                adminAccessService,
                adminRepositoryPort,
                idempotencyPort,
                timeProviderPort,
                idGeneratorPort,
                auditPort
        );
    }

    @Bean
    public AgentUpdateCardStatusUseCase agentUpdateCardStatusUseCase(
            AgentRepositoryPort agentRepositoryPort,
            TimeProviderPort timeProviderPort,
            CardRepositoryPort cardRepositoryPort,
            AuditPort auditPort
    ) {
        return new AgentUpdateCardStatusService(
                agentRepositoryPort,
                timeProviderPort,
                cardRepositoryPort,
                auditPort
        );
    }

    @Bean
    public AdminUnblockCardUseCase adminUnblockCardUseCase(
            AdminAccessService adminAccessService,
            TimeProviderPort timeProviderPort,
            CardRepositoryPort cardRepositoryPort,
            AuditPort auditPort
    ) {
        return new AdminUnblockCardService(
                adminAccessService,
                timeProviderPort,
                cardRepositoryPort,
                auditPort
        );
    }

    @Bean
    public AdminUpdateCardStatusUseCase adminUpdateCardStatusUseCase(
            AdminAccessService adminAccessService,
            TimeProviderPort timeProviderPort,
            CardRepositoryPort cardRepositoryPort,
            AuditPort auditPort
    ) {
        return new AdminUpdateCardStatusService(
                adminAccessService,
                timeProviderPort,
                cardRepositoryPort,
                auditPort
        );
    }

    @Bean
    public UpdateAgentStatusUseCase updateAgentStatusUseCase(
            AdminAccessService adminAccessService,
            AgentRepositoryPort agentRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort,
            DomainEventPublisherPort domainEventPublisherPort,
            LedgerQueryPort ledgerQueryPort
    ) {
        return new UpdateAgentStatusService(
                adminAccessService,
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
            AdminAccessService adminAccessService,
            AccountProfilePort accountProfilePort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort,
            DomainEventPublisherPort domainEventPublisherPort
    ) {
        return new UpdateAccountProfileStatusService(
                adminAccessService,
                accountProfilePort,
                auditPort,
                timeProviderPort,
                domainEventPublisherPort
        );
    }

    @Bean
    public UpdateClientStatusUseCase updateClientStatusUseCase(
            AdminAccessService adminAccessService,
            ClientRepositoryPort clientRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort,
            DomainEventPublisherPort domainEventPublisherPort,
            LedgerQueryPort ledgerQueryPort
    ) {
        return new UpdateClientStatusService(
                adminAccessService,
                clientRepositoryPort,
                auditPort,
                timeProviderPort,
                domainEventPublisherPort,
                ledgerQueryPort
        );
    }

    @Bean
    public UpdateMerchantStatusUseCase updateMerchantStatusUseCase(
            AdminAccessService adminAccessService,
            MerchantRepositoryPort merchantRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort,
            DomainEventPublisherPort domainEventPublisherPort,
            LedgerQueryPort ledgerQueryPort
    ) {
        return new UpdateMerchantStatusService(
                adminAccessService,
                merchantRepositoryPort,
                auditPort,
                timeProviderPort,
                domainEventPublisherPort,
                ledgerQueryPort
        );
    }

    @Bean
    public UpdateTerminalStatusUseCase updateTerminalStatusUseCase(
            AdminAccessService adminAccessService,
            TerminalRepositoryPort terminalRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort
    ) {
        return new UpdateTerminalStatusService(
                adminAccessService,
                terminalRepositoryPort,
                auditPort,
                timeProviderPort
        );
    }

    @Bean
    public UpdateAdminStatusUseCase updateAdminStatusUseCase(
            AdminAccessService adminAccessService,
            AdminRepositoryPort adminRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort
    ) {
        return new UpdateAdminStatusService(
                adminAccessService,
                adminRepositoryPort,
                auditPort,
                timeProviderPort
        );
    }

    @Bean
    public UpdateFeeConfigUseCase updateFeeConfigUseCase(
            AdminAccessService adminAccessService,
            FeeConfigPort feeConfigPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort
    ) {
        return new UpdateFeeConfigService(
                adminAccessService,
                feeConfigPort,
                auditPort,
                timeProviderPort
        );
    }

    @Bean
    public UpdateCommissionConfigUseCase updateCommissionConfigUseCase(
            AdminAccessService adminAccessService,
            CommissionConfigPort commissionConfigPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort
    ) {
        return new UpdateCommissionConfigService(
                adminAccessService,
                commissionConfigPort,
                auditPort,
                timeProviderPort
        );
    }

    @Bean
    public UpdatePlatformConfigUseCase updatePlatformConfigUseCase(
            AdminAccessService adminAccessService,
            PlatformConfigPort platformConfigPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort
    ) {
        return new UpdatePlatformConfigService(
                adminAccessService,
                platformConfigPort,
                auditPort,
                timeProviderPort
        );
    }

    // -----------------------------
    // Policy and Guard
    // -----------------------------

    @Bean
    public OperationAuthorizationService operationStatusGuards(AccountProfilePort accountProfilePort) {
        return new OperationAuthorizationService(accountProfilePort);
    }

    // -----------------------------
    // Read-side
    // -----------------------------

    @Bean
    public GetBalanceUseCase getBalanceUseCase(
            LedgerQueryPort ledgerQueryPort,
            AdminAccessService adminAccessService,
            LedgerOwnerRefResolver ledgerOwnerRefResolver
    ) {
        return new GetBalanceService(
                ledgerQueryPort,
                adminAccessService,
                ledgerOwnerRefResolver);
    }

    @Bean
    public SearchTransactionHistoryUseCase searchTransactionHistoryUseCase(
            AdminAccessService adminAccessService,
            LedgerQueryPort ledgerQueryPort,
            TransactionRepositoryPort transactionRepositoryPort,
            LedgerOwnerRefResolver ledgerOwnerRefResolver
    ) {
        return new SearchTransactionHistoryService(
                adminAccessService,
                ledgerQueryPort,
                transactionRepositoryPort,
                ledgerOwnerRefResolver
        );
    }

    // -----------------------------
    // Read query use-cases
    // -----------------------------

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
