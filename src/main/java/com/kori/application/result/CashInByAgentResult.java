package com.kori.application.result;

import java.math.BigDecimal;

public record CashInByAgentResult(
        String transactionId,
        String agentId,
        String clientId,
        String clientPhoneNumber,
        BigDecimal amount
) {
}
