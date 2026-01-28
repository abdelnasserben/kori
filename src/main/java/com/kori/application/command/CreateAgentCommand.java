package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

public record CreateAgentCommand(String idempotencyKey, ActorContext actorContext) {
    public CreateAgentCommand(String idempotencyKey, ActorContext actorContext) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.actorContext = Objects.requireNonNull(actorContext, "actorContext");
    }
}
