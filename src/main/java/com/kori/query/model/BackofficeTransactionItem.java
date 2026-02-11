package com.kori.query.model;

import java.math.BigDecimal;
import java.time.Instant;

public record BackofficeTransactionItem(
        String transactionId,
        String type,
        String status,
        BigDecimal amount,
        String currency,
        String merchantCode,
        String agentCode,
        String clientId,
        Instant createdAt
) {
}
