package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

public record UpdateAccountProfileStatusCommand(
        ActorContext actorContext,
        String accountType,
        String ownerRef,
        String targetStatus,
        String reason) {

    public UpdateAccountProfileStatusCommand(ActorContext actorContext, String accountType, String ownerRef, String targetStatus, String reason) {
        this.actorContext = Objects.requireNonNull(actorContext);
        this.accountType = Objects.requireNonNull(accountType);
        this.ownerRef = Objects.requireNonNull(ownerRef);
        this.targetStatus = Objects.requireNonNull(targetStatus);
        this.reason = (reason == null || reason.isBlank()) ? "N/A" : reason;
    }
}
