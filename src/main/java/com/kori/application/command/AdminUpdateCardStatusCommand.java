package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;
import java.util.UUID;

public record AdminUpdateCardStatusCommand(
        ActorContext actorContext,
        UUID cardUid,
        String targetStatus, // must be ACTIVE, INACTIVE or SUSPENDED
        String reason) {

    public AdminUpdateCardStatusCommand(ActorContext actorContext,
                                        UUID cardUid,
                                        String targetStatus,
                                        String reason) {
        this.actorContext = Objects.requireNonNull(actorContext);
        this.cardUid = Objects.requireNonNull(cardUid);
        this.targetStatus = Objects.requireNonNull(targetStatus);
        this.reason = (reason == null || reason.isBlank()) ? "N/A" : reason;
    }
}
