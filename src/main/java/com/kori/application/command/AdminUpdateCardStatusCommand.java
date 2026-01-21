package com.kori.application.command;

import com.kori.application.security.ActorContext;
import com.kori.domain.model.card.AdminCardStatusAction;

import java.util.Objects;

public record AdminUpdateCardStatusCommand(String idempotencyKey, ActorContext actorContext, String cardUid,
                                           AdminCardStatusAction action, String reason) {

    public AdminUpdateCardStatusCommand(String idempotencyKey,
                                        ActorContext actorContext,
                                        String cardUid,
                                        AdminCardStatusAction action,
                                        String reason) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.actorContext = Objects.requireNonNull(actorContext);
        this.cardUid = Objects.requireNonNull(cardUid);
        this.action = Objects.requireNonNull(action);
        this.reason = (reason == null || reason.isBlank()) ? "N/A" : reason;
    }
}
