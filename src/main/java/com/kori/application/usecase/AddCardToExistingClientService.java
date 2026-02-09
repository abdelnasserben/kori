package com.kori.application.usecase;

import com.kori.application.command.AddCardToExistingClientCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorGuards;
import com.kori.application.guard.OperationStatusGuards;
import com.kori.application.idempotency.IdempotencyExecutor;
import com.kori.application.port.in.AddCardToExistingClientUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.AddCardToExistingClientResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.client.Client;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class AddCardToExistingClientService implements AddCardToExistingClientUseCase {

    private final TimeProviderPort timeProviderPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final CardRepositoryPort cardRepositoryPort;
    private final AgentRepositoryPort agentRepositoryPort;
    private final OperationStatusGuards operationStatusGuards;
    private final CardEnrollmentWorkflow enrollmentWorkflow;
    private final AuditPort auditPort;
    private final IdempotencyExecutor idempotencyExecutor;

    public AddCardToExistingClientService(
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
        this.timeProviderPort = timeProviderPort;
        this.clientRepositoryPort = clientRepositoryPort;
        this.cardRepositoryPort = cardRepositoryPort;
        this.agentRepositoryPort = agentRepositoryPort;
        this.operationStatusGuards = operationStatusGuards;
        this.auditPort = auditPort;
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
    public AddCardToExistingClientResult execute(AddCardToExistingClientCommand command) {
        Objects.requireNonNull(command, "command");

        return idempotencyExecutor.execute(
                command.idempotencyKey(),
                command.idempotencyRequestHash(),
                AddCardToExistingClientResult.class,
                () -> {
                    // business logic

                    ActorGuards.requireAgent(command.actorContext(), "add a card to an existing client");

                    Agent agent = agentRepositoryPort.findByCode(AgentCode.of(command.agentCode()))
                            .orElseThrow(() -> new NotFoundException("Agent not found"));
                    operationStatusGuards.requireActiveAgent(agent);

                    Client client = clientRepositoryPort.findByPhoneNumber(command.phoneNumber())
                            .orElseThrow(() -> new NotFoundException("Client not found"));
                    operationStatusGuards.requireActiveClient(client);

                    // Card UID must doesn't exists
                    if (cardRepositoryPort.findByCardUid(command.cardUid()).isPresent()) {
                        throw new ForbiddenOperationException("Card UID already enrolled");
                    }

                    Instant now = timeProviderPort.now();

                    var outcome = enrollmentWorkflow.enrollCard(client, agent, command.cardUid(), command.pin(), now);

                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("transactionId", outcome.transaction().id().value().toString());
                    metadata.put("agentCode", command.agentCode());
                    metadata.put("clientId", client.id().value().toString());
                    metadata.put("cardUid", outcome.card().cardUid());

                    auditPort.publish(AuditBuilder.buildBasicAudit(
                            "ADD_CARD_TO_EXISTING_CLIENT",
                            command.actorContext(),
                            now,
                            metadata
                    ));

                    return new AddCardToExistingClientResult(
                            outcome.transaction().id().value().toString(),
                            client.id().value().toString(),
                            outcome.card().cardUid(),
                            outcome.cardPrice().asBigDecimal(),
                            outcome.agentCommission().asBigDecimal()
                    );
                }
        );
    }
}
