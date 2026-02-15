package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

public record UpdateAdminStatusCommand(
        ActorContext actorContext,
        String adminUsername,
        String targetStatus,
        String reason) {

    public UpdateAdminStatusCommand(ActorContext actorContext, String adminUsername, String targetStatus, String reason) {
        this.actorContext = Objects.requireNonNull(actorContext);
        this.adminUsername = Objects.requireNonNull(adminUsername);
        this.targetStatus = Objects.requireNonNull(targetStatus);
        this.reason = (reason == null || reason.isBlank()) ? "N/A" : reason;
    }
}
