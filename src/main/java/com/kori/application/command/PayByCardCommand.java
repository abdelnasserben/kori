package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.math.BigDecimal;
import java.util.Objects;

public record PayByCardCommand(
        String idempotencyKey,
        String idempotencyRequestHash,
        ActorContext actorContext,
        String cardUid,
        String pin,
        BigDecimal amount) {
    public PayByCardCommand(
            String idempotencyKey,
            String idempotencyRequestHash,
            ActorContext actorContext,
            String cardUid,
            String pin,
            BigDecimal amount) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.idempotencyRequestHash = Objects.requireNonNull(idempotencyRequestHash, "idempotencyRequestHash");
        this.actorContext = Objects.requireNonNull(actorContext);
        this.cardUid = Objects.requireNonNull(cardUid);
        this.pin = Objects.requireNonNull(pin);
        this.amount = Objects.requireNonNull(amount);
    }
}
