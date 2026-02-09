package com.kori.application.idempotency;

public enum IdempotencyClaimStatus {
    CLAIMED,
    ALREADY_COMPLETED,
    IN_PROGRESS,
    CONFLICT
}
