package com.kori.application.usecase;

import com.kori.application.command.UpdateClientStatusCommand;
import com.kori.application.events.ClientStatusChangedEvent;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorGuards;
import com.kori.application.port.in.UpdateClientStatusUseCase;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.ClientRepositoryPort;
import com.kori.application.port.out.DomainEventPublisherPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateClientStatusResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.application.utils.ReasonNormalizer;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Status;

import java.time.Instant;
import java.util.UUID;

public class UpdateClientStatusService implements UpdateClientStatusUseCase {

    private final ClientRepositoryPort clientRepositoryPort;
    private final AuditPort auditPort;
    private final TimeProviderPort timeProviderPort;
    private final DomainEventPublisherPort domainEventPublisherPort;

    public UpdateClientStatusService(
            ClientRepositoryPort clientRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort,
            DomainEventPublisherPort domainEventPublisherPort
    ) {
        this.clientRepositoryPort = clientRepositoryPort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
        this.domainEventPublisherPort = domainEventPublisherPort;
    }

    @Override
    public UpdateClientStatusResult execute(UpdateClientStatusCommand cmd) {
        ActorGuards.requireAdmin(cmd.actorContext(), "update client status");

        ClientId clientId = ClientId.of(cmd.clientId());
        Client client = clientRepositoryPort.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Client not found"));

        Status beforeStatus = client.status();
        String before = beforeStatus.name();

        Status afterStatus = Status.parseStatus(cmd.targetStatus());

        // Apply updating (domain validates transitions)
        switch (afterStatus) {
            case ACTIVE -> client.activate();
            case SUSPENDED -> client.suspend();
            case CLOSED -> client.close();
        }

        clientRepositoryPort.save(client);

        // Normalize reason (blank => N/A)
        String reason = ReasonNormalizer.normalize(cmd.reason());

        Instant now = timeProviderPort.now();

        // Audit
        auditPort.publish(AuditBuilder.buildStatusChangeAudit(
                "ADMIN_UPDATE_CLIENT_STATUS",
                cmd.actorContext(),
                now,
                "clientId",
                cmd.clientId(),
                before,
                cmd.targetStatus(),
                reason
        ));

        // Publish event
        if (beforeStatus != afterStatus) {
            domainEventPublisherPort.publish(new ClientStatusChangedEvent(
                    UUID.randomUUID().toString(),
                    now,
                    client.id(),
                    beforeStatus,
                    afterStatus,
                    reason
            ));
        }

        return new UpdateClientStatusResult(cmd.clientId(), before, cmd.targetStatus());
    }
}
