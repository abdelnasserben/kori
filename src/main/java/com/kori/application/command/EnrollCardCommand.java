package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

public record EnrollCardCommand(
        String idempotencyKey,
        String idempotencyRequestHash,
        ActorContext actorContext,
        String phoneNumber,
        String displayName,
        String cardUid,
        String pin) {
    public EnrollCardCommand(
            String idempotencyKey,
            String idempotencyRequestHash,
            ActorContext actorContext,
            String phoneNumber,
            String displayName,
            String cardUid,
            String pin) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.idempotencyRequestHash = Objects.requireNonNull(idempotencyRequestHash, "idempotencyRequestHash");
        this.actorContext = Objects.requireNonNull(actorContext);
        this.phoneNumber = Objects.requireNonNull(phoneNumber);
        this.displayName = displayName;
        this.cardUid = Objects.requireNonNull(cardUid);
        this.pin = Objects.requireNonNull(pin);
    }
}
