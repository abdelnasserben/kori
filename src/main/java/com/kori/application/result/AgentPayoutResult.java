package com.kori.application.result;

import com.kori.domain.model.payout.PayoutStatus;

import java.math.BigDecimal;
import java.util.Objects;

public record AgentPayoutResult(
        String transactionId,
        String payoutId,
        String agentId,
        BigDecimal amount,
        PayoutStatus payoutStatus
) {

    public AgentPayoutResult(String transactionId,
                             String payoutId,
                             String agentId,
                             BigDecimal amount,
                             PayoutStatus payoutStatus) {
        this.transactionId = Objects.requireNonNull(transactionId);
        this.payoutId = Objects.requireNonNull(payoutId);
        this.agentId = Objects.requireNonNull(agentId);
        this.amount = Objects.requireNonNull(amount);
        this.payoutStatus = Objects.requireNonNull(payoutStatus);
    }
}
