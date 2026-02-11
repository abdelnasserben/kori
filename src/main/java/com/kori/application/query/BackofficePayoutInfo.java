package com.kori.application.query;

import java.math.BigDecimal;
import java.time.Instant;

public record BackofficePayoutInfo(
        String payoutId,
        String status,
        BigDecimal amount,
        Instant createdAt,
        Instant completedAt,
        Instant failedAt,
        String failureReason
) {
}
