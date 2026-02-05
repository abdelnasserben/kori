package com.kori.application.result;

import java.math.BigDecimal;
import java.util.Objects;

public record AgentBankDepositReceiptResult(
        String transactionId,
        String agentCode,
        BigDecimal amount
) {
    public AgentBankDepositReceiptResult(String transactionId, String agentCode, BigDecimal amount) {
        this.transactionId = Objects.requireNonNull(transactionId);
        this.agentCode = Objects.requireNonNull(agentCode);
        this.amount = Objects.requireNonNull(amount);
    }
}
