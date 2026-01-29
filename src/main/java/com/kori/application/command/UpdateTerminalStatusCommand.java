package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

public record UpdateTerminalStatusCommand(
        ActorContext actorContext,
        String terminalId,
        String targetStatus,
        String reason) {

    public UpdateTerminalStatusCommand(ActorContext actorContext, String terminalId, String targetStatus, String reason) {
        this.actorContext = Objects.requireNonNull(actorContext);
        this.terminalId = Objects.requireNonNull(terminalId);
        this.targetStatus = Objects.requireNonNull(targetStatus);
        this.reason = (reason == null || reason.isBlank()) ? "N/A" : reason;
    }
}
