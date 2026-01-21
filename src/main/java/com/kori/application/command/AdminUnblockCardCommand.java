package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

public record AdminUnblockCardCommand(String idempotencyKey, ActorContext actorContext, String cardUid, String reason) {

    public AdminUnblockCardCommand(String idempotencyKey,
                                   ActorContext actorContext,
                                   String cardUid,
                                   String reason) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.actorContext = Objects.requireNonNull(actorContext);
        this.cardUid = Objects.requireNonNull(cardUid);
        this.reason = (reason == null || reason.isBlank()) ? "N/A" : reason;
    }
}
