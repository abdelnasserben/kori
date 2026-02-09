package com.kori.application.usecase;

import com.kori.application.command.EnrollCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorGuards;
import com.kori.application.guard.OperationStatusGuards;
import com.kori.application.port.in.EnrollCardUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.EnrollCardResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Status;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class EnrollCardService implements EnrollCardUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;
    private final IdGeneratorPort idGeneratorPort;

    private final ClientRepositoryPort clientRepositoryPort;
    private final CardRepositoryPort cardRepositoryPort;

    private final AgentRepositoryPort agentRepositoryPort;

    private final AccountProfilePort accountProfilePort;

    private final AuditPort auditPort;
    private final OperationStatusGuards operationStatusGuards;
    private final CardEnrollmentWorkflow enrollmentWorkflow;

    public EnrollCardService(TimeProviderPort timeProviderPort,
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
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.idGeneratorPort = idGeneratorPort;
        this.clientRepositoryPort = clientRepositoryPort;
        this.cardRepositoryPort = cardRepositoryPort;
        this.agentRepositoryPort = agentRepositoryPort;
        this.accountProfilePort = accountProfilePort;
        this.auditPort = auditPort;
        this.operationStatusGuards = operationStatusGuards;
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
    }

    @Override
    public EnrollCardResult execute(EnrollCardCommand command) {

        // 0) Idempotence
        var cached = idempotencyPort.find(command.idempotencyKey(), command.idempotencyRequestHash(), EnrollCardResult.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        // 1) Authorization: enrollment must be initiated by an AGENT
        ActorGuards.requireAgent(command.actorContext(), "enroll a card");

        // 2) Agent must exist (by code) + must be ACTIVE
        Agent agent = agentRepositoryPort.findByCode(AgentCode.of(command.agentCode()))
                .orElseThrow(() -> new NotFoundException("Agent not found"));

        operationStatusGuards.requireActiveAgent(agent);

        // 3) Card UID must be unique
        if (cardRepositoryPort.findByCardUid(command.cardUid()).isPresent()) {
            throw new ForbiddenOperationException("Card UID already enrolled");
        }

        Instant now = timeProviderPort.now();

        var inProgress = IdempotencyReservations.reserveOrLoad(
                idempotencyPort,
                command.idempotencyKey(),
                command.idempotencyRequestHash(),
                EnrollCardResult.class
        );
        if (inProgress.isPresent()) {
            return inProgress.get();
        }

        // 4) client (find or create by phone)
        boolean clientCreated = false;
        Optional<Client> existingClient = clientRepositoryPort.findByPhoneNumber(command.phoneNumber());
        existingClient.ifPresent(operationStatusGuards::requireClientEligibleForEnroll);

        Client client = existingClient.orElse(null);

        if (client == null) {
            client = Client.activeNew(new ClientId(idGeneratorPort.newUuid()), command.phoneNumber(), now);
            client = clientRepositoryPort.save(client);
            clientCreated = true;
        }

        // 5) client AccountProfile (create if absent)
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

        // 6-9) card enrollment workflow (create card, transaction, ledger entries, etc.)
        var outcome = enrollmentWorkflow.enrollCard(client, agent, command.cardUid(), command.pin(), now);

        // 10) audit
        Map<String, String> metadata = new HashMap<>();
        metadata.put("transactionId", outcome.transaction().id().value().toString());
        metadata.put("agentCode", command.agentCode());
        metadata.put("clientPhoneNumber", client.phoneNumber());
        metadata.put("cardUid", outcome.card().cardUid());

        auditPort.publish(AuditBuilder.buildBasicAudit(
                "ENROLL_CARD",
                command.actorContext(),
                now,
                metadata
        ));

        EnrollCardResult result = new EnrollCardResult(
                outcome.transaction().id().value().toString(),
                client.phoneNumber(),
                outcome.card().cardUid(),
                outcome.cardPrice().asBigDecimal(),
                outcome.agentCommission().asBigDecimal(),
                clientCreated,
                clientAccountProfileCreated
        );

        idempotencyPort.save(command.idempotencyKey(), command.idempotencyRequestHash(), result);
        return result;
    }
}
