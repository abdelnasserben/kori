package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.math.BigDecimal;
import java.util.Objects;

public record ClientTransferCommand(
        String idempotencyKey,
        String idempotencyRequestHash,
        ActorContext actorContext,
        String recipientPhoneNumber,
        BigDecimal amount
) {
    public ClientTransferCommand {
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(idempotencyRequestHash, "idempotencyRequestHash");
        Objects.requireNonNull(actorContext, "actorContext");
        Objects.requireNonNull(recipientPhoneNumber, "recipientPhoneNumber");
        Objects.requireNonNull(amount, "amount");
    }
}
