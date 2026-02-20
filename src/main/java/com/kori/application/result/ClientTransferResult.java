package com.kori.application.result;

import java.math.BigDecimal;

public record ClientTransferResult(
        String transactionId,
        String senderClientCode,
        String recipientClientCode,
        String recipientPhoneNumber,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal totalDebited
) {
}
