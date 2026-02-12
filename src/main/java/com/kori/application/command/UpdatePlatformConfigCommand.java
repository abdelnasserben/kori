package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.math.BigDecimal;
import java.util.Objects;

public record UpdatePlatformConfigCommand(
        ActorContext actorContext,
        BigDecimal agentCashLimitGlobal,
        String reason
) {
    public UpdatePlatformConfigCommand(
            ActorContext actorContext,
            BigDecimal agentCashLimitGlobal,
            String reason
    ) {
        this.actorContext = Objects.requireNonNull(actorContext, "actorContext");
        this.agentCashLimitGlobal = Objects.requireNonNull(agentCashLimitGlobal, "agentCashLimitGlobal");
        this.reason = (reason == null || reason.isBlank()) ? "N/A" : reason;
    }
}
