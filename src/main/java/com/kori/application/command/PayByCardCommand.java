package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.math.BigDecimal;
import java.util.Objects;

public record PayByCardCommand(String idempotencyKey, ActorContext actorContext, String terminalId, String cardUid,
                               String pin, BigDecimal amount) {
    public PayByCardCommand(String idempotencyKey,
                            ActorContext actorContext,
                            String terminalId,
                            String cardUid,
                            String pin,
                            BigDecimal amount) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.actorContext = Objects.requireNonNull(actorContext);
        this.terminalId = Objects.requireNonNull(terminalId);
        this.cardUid = Objects.requireNonNull(cardUid);
        this.pin = Objects.requireNonNull(pin);
        this.amount = Objects.requireNonNull(amount);
    }
}
