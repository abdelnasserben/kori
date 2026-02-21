package com.kori.application.result;

import java.math.BigDecimal;

public record PlatformConfigResult(
        BigDecimal agentCashLimitGlobal,
        BigDecimal clientTransferMinPerTransaction,
        BigDecimal clientTransferMaxPerTransaction,
        BigDecimal clientTransferDailyMax,
        BigDecimal merchantTransferMinPerTransaction,
        BigDecimal merchantTransferMaxPerTransaction,
        BigDecimal merchantTransferDailyMax,
        BigDecimal merchantWithdrawMinPerTransaction
) {
}
