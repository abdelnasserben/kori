package com.kori.application.command;

import com.kori.application.exception.ValidationException;
import com.kori.application.security.ActorContext;

import java.util.Objects;

public record FailClientRefundCommand(
        ActorContext actorContext,
        String refundId,
        String reason
) {
    public FailClientRefundCommand {
        Objects.requireNonNull(actorContext);
        if (refundId == null || refundId.isBlank()) {
            throw new ValidationException("refundId cannot be null/blank");
        }
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("reason cannot be null/blank");
        }
    }
}
