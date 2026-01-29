package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

public record UpdateAgentStatusCommand(
        ActorContext actorContext,
        String agentCode,
        String targetStatus,
        String reason) {

    public UpdateAgentStatusCommand(ActorContext actorContext, String agentCode, String targetStatus, String reason) {
        this.actorContext = Objects.requireNonNull(actorContext);
        this.agentCode = Objects.requireNonNull(agentCode);
        this.targetStatus = Objects.requireNonNull(targetStatus);
        this.reason = (reason == null || reason.isBlank()) ? "N/A" : reason;
    }
}
