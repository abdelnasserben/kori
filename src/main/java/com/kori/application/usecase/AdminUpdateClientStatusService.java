package com.kori.application.usecase;

import com.kori.application.command.AdminUpdateClientStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.AdminUpdateClientStatusUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.AdminUpdateClientStatusResult;
import com.kori.application.security.ActorType;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Status;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class AdminUpdateClientStatusService implements AdminUpdateClientStatusUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final AuditPort auditPort;

    public AdminUpdateClientStatusService(TimeProviderPort timeProviderPort,
                                          IdempotencyPort idempotencyPort,
                                          ClientRepositoryPort clientRepositoryPort,
                                          AuditPort auditPort) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.clientRepositoryPort = clientRepositoryPort;
        this.auditPort = auditPort;
    }

    @Override
    public AdminUpdateClientStatusResult execute(AdminUpdateClientStatusCommand command) {
        var cached = idempotencyPort.find(command.idempotencyKey(), AdminUpdateClientStatusResult.class);
        if (cached.isPresent()) return cached.get();

        if (command.actorContext().actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can update client status");
        }

        ClientId clientId = ClientId.of(command.clientId());
        Client client = clientRepositoryPort.findById(clientId)
                .orElseThrow(() -> new ForbiddenOperationException("Client not found"));

        Status target = switch (command.action()) {
            case ACTIVE -> Status.ACTIVE;
            case SUSPENDED -> Status.SUSPENDED;
            case CLOSED -> Status.CLOSED;
        };

        Client updated = new Client(client.id(), client.phoneNumber(), target);
        updated = clientRepositoryPort.save(updated);

        Instant now = timeProviderPort.now();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("clientId", updated.id().value());
        metadata.put("reason", command.reason());

        String auditAction = "ADMIN_SET_CLIENT_STATUS_" + command.action().name();

        auditPort.publish(new AuditEvent(
                auditAction,
                command.actorContext().actorType().name(),
                command.actorContext().actorId(),
                now,
                metadata
        ));

        AdminUpdateClientStatusResult result =
                new AdminUpdateClientStatusResult(updated.id().value(), updated.status().name());

        idempotencyPort.save(command.idempotencyKey(), result);
        return result;
    }
}
