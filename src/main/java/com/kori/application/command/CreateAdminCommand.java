package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

public record CreateAdminCommand(String idempotencyKey, String idempotencyRequestHash, ActorContext actorContext) {
    public CreateAdminCommand(String idempotencyKey, String idempotencyRequestHash, ActorContext actorContext) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.idempotencyRequestHash = Objects.requireNonNull(idempotencyRequestHash, "idempotencyRequestHash");
        this.actorContext = Objects.requireNonNull(actorContext, "actorContext");
    }
}
