package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

public record AddCardToExistingClientCommand(
        String idempotencyKey,
        String idempotencyRequestHash,
        ActorContext actorContext,
        String phoneNumber,
        String cardUid,
        String pin,
        String agentCode) {
    public AddCardToExistingClientCommand(
            String idempotencyKey,
            String idempotencyRequestHash,
            ActorContext actorContext,
            String phoneNumber,
            String cardUid,
            String pin,
            String agentCode) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.idempotencyRequestHash = Objects.requireNonNull(idempotencyRequestHash, "idempotencyRequestHash");
        this.actorContext = Objects.requireNonNull(actorContext);
        this.phoneNumber = Objects.requireNonNull(phoneNumber);
        this.cardUid = Objects.requireNonNull(cardUid);
        this.pin = Objects.requireNonNull(pin);
        this.agentCode = Objects.requireNonNull(agentCode);
    }
}
