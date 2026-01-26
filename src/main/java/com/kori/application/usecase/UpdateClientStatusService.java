package com.kori.application.usecase;

import com.kori.application.command.UpdateClientStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.UpdateClientStatusUseCase;
import com.kori.application.port.out.AuditEvent;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.ClientRepositoryPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateClientStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.common.Status;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class UpdateClientStatusService implements UpdateClientStatusUseCase {

    private final ClientRepositoryPort clientRepositoryPort;
    private final AuditPort auditPort;
    private final TimeProviderPort timeProviderPort;

    public UpdateClientStatusService(
            ClientRepositoryPort clientRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort) {
        this.clientRepositoryPort = clientRepositoryPort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
    }

    @Override
    public UpdateClientStatusResult execute(UpdateClientStatusCommand cmd) {
        requireAdmin(cmd.actorContext());

        Client client = clientRepositoryPort.findById(cmd.clientId())
                .orElseThrow(() -> new NotFoundException("Client not found"));

        // For audit
        Status before = client.status();

        // Apply updating
        switch (cmd.targetStatus()) {
            case ACTIVE -> client.activate();
            case SUSPENDED -> client.suspend();
            case CLOSED -> client.close();
        }
        clientRepositoryPort.save(client);

        // Audit
        String auditAction = "ADMIN_UPDATE_CLIENT_STATUS_" + cmd.targetStatus();
        Instant now = timeProviderPort.now();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("clientId", cmd.clientId().toString());
        metadata.put("before", before.name());
        metadata.put("after", cmd.targetStatus().name());
        metadata.put("reason", cmd.reason());

        auditPort.publish(new AuditEvent(
                auditAction,
                cmd.actorContext().actorType().name(),
                cmd.actorContext().actorId(),
                now,
                metadata
        ));
        return new UpdateClientStatusResult(client.id(), before, cmd.targetStatus());
    }

    private void requireAdmin(ActorContext actor) {
        if (actor == null || actor.actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can update client status");
        }
    }
}
