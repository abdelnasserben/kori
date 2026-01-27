package com.kori.application.result;

import java.math.BigDecimal;
import java.util.Objects;

public record AgentPayoutResult(
        String transactionId,
        String payoutId,
        String agentCode,
        BigDecimal amount,
        String payoutStatus
) {

    public AgentPayoutResult(String transactionId,
                             String payoutId,
                             String agentCode,
                             BigDecimal amount,
                             String payoutStatus) {
        this.transactionId = Objects.requireNonNull(transactionId);
        this.payoutId = Objects.requireNonNull(payoutId);
        this.agentCode = Objects.requireNonNull(agentCode);
        this.amount = Objects.requireNonNull(amount);
        this.payoutStatus = Objects.requireNonNull(payoutStatus);
    }
}
