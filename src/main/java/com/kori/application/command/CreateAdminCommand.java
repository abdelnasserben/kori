package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

public record CreateAdminCommand(String idempotencyKey, String idempotencyRequestHash, ActorContext actorContext, String username, String displayName) {
    public CreateAdminCommand(String idempotencyKey, String idempotencyRequestHash, ActorContext actorContext, String username, String displayName) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.idempotencyRequestHash = Objects.requireNonNull(idempotencyRequestHash, "idempotencyRequestHash");
        this.actorContext = Objects.requireNonNull(actorContext, "actorContext");
        this.username = Objects.requireNonNull(username, "username");
        this.displayName = displayName;
    }
}
