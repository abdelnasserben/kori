package com.kori.application.result;

import java.math.BigDecimal;

public record UpdateCommissionConfigResult(
        BigDecimal cardEnrollmentAgentCommission,
        BigDecimal merchantWithdrawCommissionRate,
        BigDecimal merchantWithdrawCommissionMin,
        BigDecimal merchantWithdrawCommissionMax
) {
}
