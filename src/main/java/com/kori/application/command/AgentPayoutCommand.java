package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

/**
 * ADMIN-only command.
 * Amount is NOT provided by caller: payout amount is computed as "exactly what is due" from the ledger.
 */
public record AgentPayoutCommand(String idempotencyKey, ActorContext actorContext, String agentId) {

    public AgentPayoutCommand(String idempotencyKey,
                              ActorContext actorContext,
                              String agentId) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.actorContext = Objects.requireNonNull(actorContext);
        this.agentId = Objects.requireNonNull(agentId);
    }
}
