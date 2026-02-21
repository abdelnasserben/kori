package com.kori.application.result;

import java.math.BigDecimal;

public record MerchantTransferResult(
        String transactionId,
        String senderMerchantCode,
        String recipientMerchantCode,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal totalDebited
) {
}
