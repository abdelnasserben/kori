package com.kori.domain.model.config;

import java.math.BigDecimal;
import java.util.Objects;

public record CommissionConfig(
        BigDecimal cardEnrollmentAgentCommission,
        BigDecimal merchantWithdrawCommissionRate,
        BigDecimal merchantWithdrawCommissionMin,
        BigDecimal merchantWithdrawCommissionMax
) {
    public CommissionConfig {
        Objects.requireNonNull(cardEnrollmentAgentCommission, "cardEnrollmentAgentCommission");
        Objects.requireNonNull(merchantWithdrawCommissionRate, "merchantWithdrawCommissionRate");
    }
}
