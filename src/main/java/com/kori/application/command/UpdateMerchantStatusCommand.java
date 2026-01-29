package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

public record UpdateMerchantStatusCommand(
        ActorContext actorContext,
        String merchantCode,
        String targetStatus,
        String reason) {

    public UpdateMerchantStatusCommand(ActorContext actorContext, String merchantCode, String targetStatus, String reason) {
        this.actorContext = Objects.requireNonNull(actorContext);
        this.merchantCode = Objects.requireNonNull(merchantCode);
        this.targetStatus = Objects.requireNonNull(targetStatus);
        this.reason = (reason == null || reason.isBlank()) ? "N/A" : reason;
    }
}
