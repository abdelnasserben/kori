package com.kori.application.result;

import java.math.BigDecimal;
import java.util.Objects;

public record AgentPayoutResult(String transactionId, String agentId, BigDecimal amount) {

    public AgentPayoutResult(String transactionId,
                             String agentId,
                             BigDecimal amount) {
        this.transactionId = Objects.requireNonNull(transactionId);
        this.agentId = Objects.requireNonNull(agentId);
        this.amount = Objects.requireNonNull(amount);
    }
}
