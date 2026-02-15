package com.kori.application.result;

import java.math.BigDecimal;
import java.util.Objects;

public record ClientRefundResult(
        String transactionId,
        String refundId,
        String clientCode,
        BigDecimal amount,
        String refundStatus
) {
    public ClientRefundResult {
        Objects.requireNonNull(transactionId);
        Objects.requireNonNull(refundId);
        Objects.requireNonNull(clientCode);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(refundStatus);
    }
}
