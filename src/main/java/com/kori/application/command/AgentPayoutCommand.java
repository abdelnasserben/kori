package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.math.BigDecimal;
import java.util.Objects;

public record AgentPayoutCommand(String idempotencyKey, ActorContext actorContext, String agentId, BigDecimal amount) {

    public AgentPayoutCommand(String idempotencyKey,
                              ActorContext actorContext,
                              String agentId,
                              BigDecimal amount) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.actorContext = Objects.requireNonNull(actorContext);
        this.agentId = Objects.requireNonNull(agentId);
        this.amount = Objects.requireNonNull(amount);
    }
}
