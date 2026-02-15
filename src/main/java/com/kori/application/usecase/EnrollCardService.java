package com.kori.application.usecase;

import com.kori.application.command.EnrollCardCommand;
import com.kori.application.exception.*;
import com.kori.application.guard.ActorStatusGuards;
import com.kori.application.guard.ActorTypeGuards;
import com.kori.application.idempotency.IdempotencyExecutor;
import com.kori.application.port.in.EnrollCardUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.EnrollCardResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.ClientCode;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.client.PhoneNumber;
import com.kori.domain.model.common.Status;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class EnrollCardService implements EnrollCardUseCase {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 20;

    private final TimeProviderPort timeProviderPort;
    private final IdGeneratorPort idGeneratorPort;
    private final CodeGeneratorPort codeGeneratorPort;

    private final ClientRepositoryPort clientRepositoryPort;
    private final CardRepositoryPort cardRepositoryPort;

    private final AgentRepositoryPort agentRepositoryPort;

    private final AccountProfilePort accountProfilePort;

    private final AuditPort auditPort;
    private final OperationAuthorizationService operationAuthorizationService;
    private final CardEnrollmentWorkflow enrollmentWorkflow;
    private final IdempotencyExecutor idempotencyExecutor;

    public EnrollCardService(TimeProviderPort timeProviderPort,
                             IdempotencyPort idempotencyPort,
                             IdGeneratorPort idGeneratorPort, CodeGeneratorPort codeGeneratorPort,
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
        this.timeProviderPort = timeProviderPort;
        this.idGeneratorPort = idGeneratorPort;
        this.codeGeneratorPort = codeGeneratorPort;
        this.clientRepositoryPort = clientRepositoryPort;
        this.cardRepositoryPort = cardRepositoryPort;
        this.agentRepositoryPort = agentRepositoryPort;
        this.accountProfilePort = accountProfilePort;
        this.auditPort = auditPort;
        this.operationAuthorizationService = operationAuthorizationService;
        this.enrollmentWorkflow = new CardEnrollmentWorkflow(
                idGeneratorPort,
                cardRepositoryPort,
                agentRepositoryPort,
                transactionRepositoryPort,
                feePolicyPort,
                commissionPolicyPort,
                ledgerAppendPort,
                ledgerQueryPort,
                platformConfigPort,
                pinHasherPort
        );

        this.idempotencyExecutor = new IdempotencyExecutor(idempotencyPort);
    }

    @Override
    public EnrollCardResult execute(EnrollCardCommand command) {
        return idempotencyExecutor.execute(
                command.idempotencyKey(),
                command.idempotencyRequestHash(),
                EnrollCardResult.class,
                () -> {

                    ActorTypeGuards.onlyAgentCan(command.actorContext(), "enroll a card");

                    String agentCode = command.actorContext().actorRef();
                    Agent agent = agentRepositoryPort.findByCode(AgentCode.of(agentCode))
                            .orElseThrow(() -> new NotFoundException("Agent not found"));

                    operationAuthorizationService.authorizeAgentOperation(agent);

                    // Card UID must be unique
                    if (cardRepositoryPort.findByCardUid(command.cardUid()).isPresent()) {
                        throw new ForbiddenOperationException("Card UID already enrolled");
                    }

                    Instant now = timeProviderPort.now();

                    // Client (find or create by phone)
                    boolean clientCreated = false;
                    Optional<Client> existingClient = clientRepositoryPort.findByPhoneNumber(PhoneNumber.of(command.phoneNumber()));
                    existingClient.ifPresent(ActorStatusGuards::requireActiveClient);

                    Client client = existingClient.orElse(null);

                    if (client == null) {
                        client = Client.activeNew(
                                new ClientId(idGeneratorPort.newUuid()),
                                generateUniqueClientCode(),
                                PhoneNumber.of(command.phoneNumber()),
                                now
                        );
                        client = clientRepositoryPort.save(client);
                        clientCreated = true;
                    }

                    // Client AccountProfile (create if absent)
                    boolean clientAccountProfileCreated = false;
                    var clientAccount = LedgerAccountRef.client(client.id().value().toString());
                    var clientProfileOpt = accountProfilePort.findByAccount(clientAccount);

                    if (clientProfileOpt.isEmpty()) {
                        AccountProfile profile = AccountProfile.activeNew(clientAccount, now);
                        accountProfilePort.save(profile);
                        clientAccountProfileCreated = true;
                    } else {
                        if (clientProfileOpt.get().status() != Status.ACTIVE) {
                            throw new ForbiddenOperationException("Client account is not active");
                        }
                    }

                    // Card enrollment workflow (create card, transaction, ledger entries, etc.)
                    var outcome = enrollmentWorkflow.enrollCard(client, agent, command.cardUid(), command.pin(), now);

                    // Audit
                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("transactionId", outcome.transaction().id().value().toString());
                    metadata.put("clientPhoneNumber", client.phoneNumber().value());
                    metadata.put("cardUid", outcome.card().cardUid());

                    auditPort.publish(AuditBuilder.buildBasicAudit(
                            "ENROLL_CARD",
                            command.actorContext(),
                            now,
                            metadata
                    ));

                    return new EnrollCardResult(
                            outcome.transaction().id().value().toString(),
                            client.phoneNumber().value(),
                            outcome.card().cardUid(),
                            outcome.cardPrice().asBigDecimal(),
                            outcome.agentCommission().asBigDecimal(),
                            clientCreated,
                            clientAccountProfileCreated
                    );
                }
        );
    }

    private ClientCode generateUniqueClientCode() {
        for (int i = 0; i < MAX_CODE_GENERATION_ATTEMPTS; i++) {
            String digits = codeGeneratorPort.next6Digits();
            ClientCode candidate = ClientCode.of("C-" + digits);
            if (!clientRepositoryPort.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new ApplicationException(
                ApplicationErrorCode.TECHNICAL_FAILURE,
                ApplicationErrorCategory.TECHNICAL,
                "Unable to generate unique clientCode."
        );
    }
}
