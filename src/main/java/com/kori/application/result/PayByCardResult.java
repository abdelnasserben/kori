package com.kori.application.result;

import java.math.BigDecimal;
import java.util.Objects;

public record PayByCardResult(String transactionId, String merchantCode, String cardUid, BigDecimal amount,
                              BigDecimal fee, BigDecimal totalDebited) {
    public PayByCardResult(String transactionId,
                           String merchantCode,
                           String cardUid,
                           BigDecimal amount,
                           BigDecimal fee,
                           BigDecimal totalDebited) {
        this.transactionId = Objects.requireNonNull(transactionId);
        this.merchantCode = Objects.requireNonNull(merchantCode);
        this.cardUid = Objects.requireNonNull(cardUid);
        this.amount = Objects.requireNonNull(amount);
        this.fee = Objects.requireNonNull(fee);
        this.totalDebited = Objects.requireNonNull(totalDebited);
    }
}
