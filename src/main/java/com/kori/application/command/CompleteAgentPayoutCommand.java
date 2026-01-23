package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

public record CompleteAgentPayoutCommand(
        ActorContext actorContext,
        String payoutId
) {
    public CompleteAgentPayoutCommand {
        Objects.requireNonNull(actorContext);
        if (payoutId == null || payoutId.isBlank()) {
            throw new IllegalArgumentException("payoutId cannot be null/blank");
        }
    }
}
