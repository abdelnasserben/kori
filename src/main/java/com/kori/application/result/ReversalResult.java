package com.kori.application.result;

import java.util.Objects;

public record ReversalResult(String reversalTransactionId, String originalTransactionId) {
    public ReversalResult(String reversalTransactionId, String originalTransactionId) {
        this.reversalTransactionId = Objects.requireNonNull(reversalTransactionId);
        this.originalTransactionId = Objects.requireNonNull(originalTransactionId);
    }
}
