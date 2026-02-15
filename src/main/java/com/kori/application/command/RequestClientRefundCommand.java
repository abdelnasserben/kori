package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

public record RequestClientRefundCommand(
        String idempotencyKey,
        String idempotencyRequestHash,
        ActorContext actorContext,
        String clientCode
) {
    public RequestClientRefundCommand {
        Objects.requireNonNull(idempotencyKey);
        Objects.requireNonNull(idempotencyRequestHash);
        Objects.requireNonNull(actorContext);
        Objects.requireNonNull(clientCode);
    }
}
