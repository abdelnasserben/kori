package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.math.BigDecimal;
import java.util.Objects;

public record UpdateCommissionConfigCommand(
        ActorContext actorContext,
        BigDecimal cardEnrollmentAgentCommission,
        BigDecimal merchantWithdrawCommissionRate,
        BigDecimal merchantWithdrawCommissionMin,
        BigDecimal merchantWithdrawCommissionMax,
        String reason
) {
    public UpdateCommissionConfigCommand(
            ActorContext actorContext,
            BigDecimal cardEnrollmentAgentCommission,
            BigDecimal merchantWithdrawCommissionRate,
            BigDecimal merchantWithdrawCommissionMin,
            BigDecimal merchantWithdrawCommissionMax,
            String reason) {
        this.actorContext = Objects.requireNonNull(actorContext, "actorContext");
        this.cardEnrollmentAgentCommission = Objects.requireNonNull(cardEnrollmentAgentCommission, "cardEnrollmentAgentCommission");
        this.merchantWithdrawCommissionRate = Objects.requireNonNull(merchantWithdrawCommissionRate, "merchantWithdrawCommissionRate");
        this.merchantWithdrawCommissionMin = merchantWithdrawCommissionMin;
        this.merchantWithdrawCommissionMax = merchantWithdrawCommissionMax;
        this.reason = (reason == null || reason.isBlank()) ? "N/A" : reason;
    }
}
