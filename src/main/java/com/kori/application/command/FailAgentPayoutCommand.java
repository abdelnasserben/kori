package com.kori.application.command;

import com.kori.application.exception.ValidationException;
import com.kori.application.security.ActorContext;

import java.util.Objects;

public record FailAgentPayoutCommand(
        ActorContext actorContext,
        String payoutId,
        String reason
) {
    public FailAgentPayoutCommand {
        Objects.requireNonNull(actorContext);
        if (payoutId == null || payoutId.isBlank()) {
            throw new ValidationException("payoutId cannot be null/blank");
        }
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("payoutId cannot be null/blank");
        }
    }
}
