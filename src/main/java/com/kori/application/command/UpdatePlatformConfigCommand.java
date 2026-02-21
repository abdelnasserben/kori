package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.math.BigDecimal;
import java.util.Objects;

public record UpdatePlatformConfigCommand(
        ActorContext actorContext,
        BigDecimal agentCashLimitGlobal,
        BigDecimal clientTransferMinPerTransaction,
        BigDecimal clientTransferMaxPerTransaction,
        BigDecimal clientTransferDailyMax,
        BigDecimal merchantTransferMinPerTransaction,
        BigDecimal merchantTransferMaxPerTransaction,
        BigDecimal merchantTransferDailyMax,
        BigDecimal merchantWithdrawMinPerTransaction,
        String reason
) {
    public UpdatePlatformConfigCommand(
            ActorContext actorContext,
            BigDecimal agentCashLimitGlobal,
            BigDecimal clientTransferMinPerTransaction,
            BigDecimal clientTransferMaxPerTransaction,
            BigDecimal clientTransferDailyMax,
            BigDecimal merchantTransferMinPerTransaction,
            BigDecimal merchantTransferMaxPerTransaction,
            BigDecimal merchantTransferDailyMax,
            BigDecimal merchantWithdrawMinPerTransaction,
            String reason
    ) {
        this.actorContext = Objects.requireNonNull(actorContext, "actorContext");
        this.agentCashLimitGlobal = Objects.requireNonNull(agentCashLimitGlobal, "agentCashLimitGlobal");
        this.clientTransferMinPerTransaction = Objects.requireNonNull(clientTransferMinPerTransaction, "clientTransferMinPerTransaction");
        this.clientTransferMaxPerTransaction = Objects.requireNonNull(clientTransferMaxPerTransaction, "clientTransferMaxPerTransaction");
        this.clientTransferDailyMax = Objects.requireNonNull(clientTransferDailyMax, "clientTransferDailyMax");
        this.merchantTransferMinPerTransaction = Objects.requireNonNull(merchantTransferMinPerTransaction, "merchantTransferMinPerTransaction");
        this.merchantTransferMaxPerTransaction = Objects.requireNonNull(merchantTransferMaxPerTransaction, "merchantTransferMaxPerTransaction");
        this.merchantTransferDailyMax = Objects.requireNonNull(merchantTransferDailyMax, "merchantTransferDailyMax");
        this.merchantWithdrawMinPerTransaction = Objects.requireNonNull(merchantWithdrawMinPerTransaction, "merchantWithdrawMinPerTransaction");
        this.reason = (reason == null || reason.isBlank()) ? "N/A" : reason;
    }
}
