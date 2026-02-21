package com.kori.domain.model.config;

import java.math.BigDecimal;
import java.util.Objects;

public record PlatformConfig(
        BigDecimal agentCashLimitGlobal,
        BigDecimal clientTransferMinPerTransaction,
        BigDecimal clientTransferMaxPerTransaction,
        BigDecimal clientTransferDailyMax,
        BigDecimal merchantTransferMinPerTransaction,
        BigDecimal merchantTransferMaxPerTransaction,
        BigDecimal merchantTransferDailyMax,
        BigDecimal merchantWithdrawMinPerTransaction
) {
    public PlatformConfig {
        Objects.requireNonNull(agentCashLimitGlobal, "agentCashLimitGlobal");
        Objects.requireNonNull(clientTransferMinPerTransaction, "clientTransferMinPerTransaction");
        Objects.requireNonNull(clientTransferMaxPerTransaction, "clientTransferMaxPerTransaction");
        Objects.requireNonNull(clientTransferDailyMax, "clientTransferDailyMax");
        Objects.requireNonNull(merchantTransferMinPerTransaction, "merchantTransferMinPerTransaction");
        Objects.requireNonNull(merchantTransferMaxPerTransaction, "merchantTransferMaxPerTransaction");
        Objects.requireNonNull(merchantTransferDailyMax, "merchantTransferDailyMax");
        Objects.requireNonNull(merchantWithdrawMinPerTransaction, "merchantWithdrawMinPerTransaction");
    }
}
