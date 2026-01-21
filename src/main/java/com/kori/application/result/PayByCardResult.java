package com.kori.application.result;

import java.math.BigDecimal;
import java.util.Objects;

public record PayByCardResult(String transactionId, String merchantId, String clientId, BigDecimal amount,
                              BigDecimal fee, BigDecimal totalDebited) {
    public PayByCardResult(String transactionId,
                           String merchantId,
                           String clientId,
                           BigDecimal amount,
                           BigDecimal fee,
                           BigDecimal totalDebited) {
        this.transactionId = Objects.requireNonNull(transactionId);
        this.merchantId = Objects.requireNonNull(merchantId);
        this.clientId = Objects.requireNonNull(clientId);
        this.amount = Objects.requireNonNull(amount);
        this.fee = Objects.requireNonNull(fee);
        this.totalDebited = Objects.requireNonNull(totalDebited);
    }
}
