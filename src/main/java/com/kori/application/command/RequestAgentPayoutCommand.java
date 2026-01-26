package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

/**
 * ADMIN-only command.
 * Amount is NOT provided by caller: payout amount is computed as "exactly what is due" from the ledger.
 */
public record RequestAgentPayoutCommand(
        String idempotencyKey,
        ActorContext actorContext,
        String agentCode) {

    public RequestAgentPayoutCommand(String idempotencyKey,
                                     ActorContext actorContext,
                                     String agentCode) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.actorContext = Objects.requireNonNull(actorContext);
        this.agentCode = Objects.requireNonNull(agentCode);
    }
}
