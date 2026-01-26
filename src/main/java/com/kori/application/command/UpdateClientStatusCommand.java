package com.kori.application.command;

import com.kori.application.security.ActorContext;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Status;

import java.util.Objects;
import java.util.UUID;

public record UpdateClientStatusCommand(
        ActorContext actorContext,
        ClientId clientId,
        Status targetStatus,
        String reason) {

    public UpdateClientStatusCommand(ActorContext actorContext, ClientId clientId, Status targetStatus, String reason) {
        this.actorContext = Objects.requireNonNull(actorContext);
        this.clientId = Objects.requireNonNull(clientId);
        this.targetStatus = Objects.requireNonNull(targetStatus);
        this.reason = (reason == null || reason.isBlank()) ? "N/A" : reason;
    }
}
