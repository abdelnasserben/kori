package com.kori.application.query;

import java.math.BigDecimal;
import java.time.Instant;

public record BackofficeClientRefundInfo(
        String refundId,
        String status,
        BigDecimal amount,
        Instant createdAt,
        Instant completedAt,
        Instant failedAt,
        String failureReason
) {
}
