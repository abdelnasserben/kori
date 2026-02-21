package com.kori.domain.model.config;

import java.math.BigDecimal;
import java.util.Objects;

public record PlatformConfig(
        BigDecimal agentCashLimitGlobal,
        BigDecimal clientTransferMaxPerTransaction,
        BigDecimal clientTransferDailyMax,
        BigDecimal merchantTransferMaxPerTransaction,
        BigDecimal merchantTransferDailyMax
) {
    public PlatformConfig {
        Objects.requireNonNull(agentCashLimitGlobal, "agentCashLimitGlobal");
        Objects.requireNonNull(clientTransferMaxPerTransaction, "clientTransferMaxPerTransaction");
        Objects.requireNonNull(clientTransferDailyMax, "clientTransferDailyMax");
        Objects.requireNonNull(merchantTransferMaxPerTransaction, "merchantTransferMaxPerTransaction");
        Objects.requireNonNull(merchantTransferDailyMax, "merchantTransferDailyMax");
    }
}
