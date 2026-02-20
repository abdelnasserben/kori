package com.kori.domain.model.config;

import java.math.BigDecimal;
import java.util.Objects;

public record PlatformConfig(
        BigDecimal agentCashLimitGlobal,
        BigDecimal clientTransferMaxPerTransaction,
        BigDecimal clientTransferDailyMax
) {
    public PlatformConfig {
        Objects.requireNonNull(agentCashLimitGlobal, "agentCashLimitGlobal");
        Objects.requireNonNull(clientTransferMaxPerTransaction, "clientTransferMaxPerTransaction");
        Objects.requireNonNull(clientTransferDailyMax, "clientTransferDailyMax");
    }
}
