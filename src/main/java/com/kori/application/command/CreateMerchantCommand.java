package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

public record CreateMerchantCommand(String idempotencyKey, ActorContext actorContext) {
    public CreateMerchantCommand(String idempotencyKey, ActorContext actorContext) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.actorContext = Objects.requireNonNull(actorContext, "actorContext");
    }
}
