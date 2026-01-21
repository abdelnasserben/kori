package com.kori.application.command;

import com.kori.application.security.ActorContext;
import com.kori.domain.model.client.AdminClientStatusAction;

import java.util.Objects;

public record AdminUpdateClientStatusCommand(
        String idempotencyKey,
        ActorContext actorContext,
        String clientId,
        AdminClientStatusAction action,
        String reason
) {
    public AdminUpdateClientStatusCommand(String idempotencyKey,
                                          ActorContext actorContext,
                                          String clientId,
                                          AdminClientStatusAction action,
                                          String reason) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.actorContext = Objects.requireNonNull(actorContext);
        this.clientId = Objects.requireNonNull(clientId);
        this.action = Objects.requireNonNull(action);
        this.reason = (reason == null || reason.isBlank()) ? "N/A" : reason;
    }
}
