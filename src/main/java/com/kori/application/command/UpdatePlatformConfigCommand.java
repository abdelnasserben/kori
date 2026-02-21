package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.math.BigDecimal;
import java.util.Objects;

public record UpdatePlatformConfigCommand(
        ActorContext actorContext,
        BigDecimal agentCashLimitGlobal,
        BigDecimal clientTransferMaxPerTransaction,
        BigDecimal clientTransferDailyMax,
        BigDecimal merchantTransferMaxPerTransaction,
        BigDecimal merchantTransferDailyMax,
        String reason
) {
    public UpdatePlatformConfigCommand(
            ActorContext actorContext,
            BigDecimal agentCashLimitGlobal,
            BigDecimal clientTransferMaxPerTransaction,
            BigDecimal clientTransferDailyMax,
            BigDecimal merchantTransferMaxPerTransaction,
            BigDecimal merchantTransferDailyMax,
            String reason
    ) {
        this.actorContext = Objects.requireNonNull(actorContext, "actorContext");
        this.agentCashLimitGlobal = Objects.requireNonNull(agentCashLimitGlobal, "agentCashLimitGlobal");
        this.clientTransferMaxPerTransaction = Objects.requireNonNull(clientTransferMaxPerTransaction, "clientTransferMaxPerTransaction");
        this.clientTransferDailyMax = Objects.requireNonNull(clientTransferDailyMax, "clientTransferDailyMax");
        this.merchantTransferMaxPerTransaction = Objects.requireNonNull(merchantTransferMaxPerTransaction, "merchantTransferMaxPerTransaction");
        this.merchantTransferDailyMax = Objects.requireNonNull(merchantTransferDailyMax, "merchantTransferDailyMax");
        this.reason = (reason == null || reason.isBlank()) ? "N/A" : reason;
    }
}
