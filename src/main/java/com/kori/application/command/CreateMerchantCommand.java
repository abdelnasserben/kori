package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

public record CreateMerchantCommand(String idempotencyKey, String idempotencyRequestHash, ActorContext actorContext, String displayName) {
    public CreateMerchantCommand(String idempotencyKey, String idempotencyRequestHash, ActorContext actorContext, String displayName) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.idempotencyRequestHash = Objects.requireNonNull(idempotencyRequestHash, "idempotencyRequestHash");
        this.actorContext = Objects.requireNonNull(actorContext, "actorContext");
        this.displayName = displayName;
    }
}
