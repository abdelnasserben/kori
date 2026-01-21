package com.kori.application.command;

import com.kori.application.security.ActorContext;
import com.kori.domain.model.card.AgentCardAction;

import java.util.Objects;

public record AgentUpdateCardStatusCommand(String idempotencyKey, ActorContext actorContext, String agentId,
                                           String cardUid, AgentCardAction action, String reason) {

    public AgentUpdateCardStatusCommand(String idempotencyKey,
                                        ActorContext actorContext,
                                        String agentId,
                                        String cardUid,
                                        AgentCardAction action,
                                        String reason) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.actorContext = Objects.requireNonNull(actorContext);
        this.agentId = Objects.requireNonNull(agentId);
        this.cardUid = Objects.requireNonNull(cardUid);
        this.action = Objects.requireNonNull(action);
        this.reason = (reason == null || reason.isBlank()) ? "N/A" : reason;
    }
}
