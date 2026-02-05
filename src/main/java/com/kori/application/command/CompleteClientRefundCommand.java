package com.kori.application.command;

import com.kori.application.exception.ValidationException;
import com.kori.application.security.ActorContext;

import java.util.Objects;

public record CompleteClientRefundCommand(
        ActorContext actorContext,
        String refundId
) {
    public CompleteClientRefundCommand {
        Objects.requireNonNull(actorContext);
        if (refundId == null || refundId.isBlank()) {
            throw new ValidationException("refundId cannot be null/blank");
        }
    }
}
