package com.kori.query.model;

import java.math.BigDecimal;
import java.time.Instant;

public record BackofficePayoutInfo(
        String payoutRef,
        String status,
        BigDecimal amount,
        Instant createdAt,
        Instant completedAt,
        Instant failedAt,
        String failureReason
) {
}
