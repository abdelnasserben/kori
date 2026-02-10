package com.kori.adapters.out.jpa.query;

import java.math.BigDecimal;
import java.time.Instant;

public record BackofficeTransactionDetails(
        String transactionId,
        String type,
        String status,
        BigDecimal amount,
        String currency,
        String merchantCode,
        String agentCode,
        String clientId,
        String originalTransactionId,
        Instant createdAt
) {
}
