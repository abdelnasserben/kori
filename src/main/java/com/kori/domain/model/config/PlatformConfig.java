package com.kori.domain.model.config;

import java.math.BigDecimal;
import java.util.Objects;

public record PlatformConfig(BigDecimal agentCashLimitGlobal) {
    public PlatformConfig {
        Objects.requireNonNull(agentCashLimitGlobal, "agentCashLimitGlobal");
    }
}
