package com.kori.application.result;

import com.kori.domain.model.payout.PayoutStatus;

import java.math.BigDecimal;
import java.util.Objects;

public record AgentPayoutResult(
        String transactionId,
        String payoutId,
        String agentCode,
        BigDecimal amount,
        PayoutStatus payoutStatus
) {

    public AgentPayoutResult(String transactionId,
                             String payoutId,
                             String agentCode,
                             BigDecimal amount,
                             PayoutStatus payoutStatus) {
        this.transactionId = Objects.requireNonNull(transactionId);
        this.payoutId = Objects.requireNonNull(payoutId);
        this.agentCode = Objects.requireNonNull(agentCode);
        this.amount = Objects.requireNonNull(amount);
        this.payoutStatus = Objects.requireNonNull(payoutStatus);
    }
}
