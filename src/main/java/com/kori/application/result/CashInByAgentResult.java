package com.kori.application.result;

import java.math.BigDecimal;

public record CashInByAgentResult(
        String transactionId,
        String agentCode,
        String clientCode,
        String clientPhoneNumber,
        BigDecimal amount
) {
}
