package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

public record EnrollCardCommand(String idempotencyKey, ActorContext actorContext, String phoneNumber, String cardUid,
                                String pin, String agentId) {
    public EnrollCardCommand(String idempotencyKey,
                             ActorContext actorContext,
                             String phoneNumber,
                             String cardUid,
                             String pin,
                             String agentId) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.actorContext = Objects.requireNonNull(actorContext);
        this.phoneNumber = Objects.requireNonNull(phoneNumber);
        this.cardUid = Objects.requireNonNull(cardUid);
        this.pin = Objects.requireNonNull(pin);
        this.agentId = Objects.requireNonNull(agentId);
    }
}
