package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

public record UpdateClientStatusCommand(
        ActorContext actorContext,
        String clientId,
        String targetStatus,
        String reason) {

    public UpdateClientStatusCommand(ActorContext actorContext, String clientId, String targetStatus, String reason) {
        this.actorContext = Objects.requireNonNull(actorContext);
        this.clientId = Objects.requireNonNull(clientId);
        this.targetStatus = Objects.requireNonNull(targetStatus);
        this.reason = (reason == null || reason.isBlank()) ? "N/A" : reason;
    }
}
