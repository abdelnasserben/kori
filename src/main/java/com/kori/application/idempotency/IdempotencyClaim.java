package com.kori.application.idempotency;

import java.util.Optional;

public record IdempotencyClaim<T>(IdempotencyClaimStatus status, Optional<T> result) {

    public static <T> IdempotencyClaim<T> claimed() {
        return new IdempotencyClaim<>(IdempotencyClaimStatus.CLAIMED, Optional.empty());
    }

    public static <T> IdempotencyClaim<T> completed(T result) {
        return new IdempotencyClaim<>(IdempotencyClaimStatus.ALREADY_COMPLETED, Optional.of(result));
    }

    public static <T> IdempotencyClaim<T> inProgress() {
        return new IdempotencyClaim<>(IdempotencyClaimStatus.IN_PROGRESS, Optional.empty());
    }

    public static <T> IdempotencyClaim<T> conflict() {
        return new IdempotencyClaim<>(IdempotencyClaimStatus.CONFLICT, Optional.empty());
    }
}
