package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

public record ReversalCommand(String idempotencyKey, ActorContext actorContext, String originalTransactionId) {
    public ReversalCommand(String idempotencyKey,
                           ActorContext actorContext,
                           String originalTransactionId) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.actorContext = Objects.requireNonNull(actorContext);
        this.originalTransactionId = Objects.requireNonNull(originalTransactionId);
    }
}
