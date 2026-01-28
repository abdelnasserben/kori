package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

public record CreateTerminalCommand(String idempotencyKey, ActorContext actorContext, String merchantCode) {
    public CreateTerminalCommand(String idempotencyKey, ActorContext actorContext, String merchantCode) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.actorContext = Objects.requireNonNull(actorContext);
        this.merchantCode = Objects.requireNonNull(merchantCode, "merchantCode");
    }
}
