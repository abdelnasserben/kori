package com.kori.application.command;

import com.kori.application.security.ActorContext;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.card.CardStatus;

import java.util.Objects;
import java.util.UUID;

public record AgentUpdateCardStatusCommand(
        ActorContext actorContext,
        UUID cardUid,
        AgentCode agentCode,
        CardStatus targetStatus, // must be ACTIVE, INACTIVE or SUSPENDED
        String reason) {

    public AgentUpdateCardStatusCommand(ActorContext actorContext,
                                        UUID cardUid,
                                        AgentCode agentCode,
                                        CardStatus targetStatus,
                                        String reason) {
        this.actorContext = Objects.requireNonNull(actorContext);
        this.cardUid = Objects.requireNonNull(cardUid);
        this.agentCode = Objects.requireNonNull(agentCode);
        this.targetStatus = Objects.requireNonNull(targetStatus);
        this.reason = (reason == null || reason.isBlank()) ? "N/A" : reason;
    }
}
