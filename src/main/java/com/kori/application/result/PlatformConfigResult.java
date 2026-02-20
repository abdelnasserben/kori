package com.kori.application.result;

import java.math.BigDecimal;

public record PlatformConfigResult(
        BigDecimal agentCashLimitGlobal,
        BigDecimal clientTransferMaxPerTransaction,
        BigDecimal clientTransferDailyMax
) {
}
