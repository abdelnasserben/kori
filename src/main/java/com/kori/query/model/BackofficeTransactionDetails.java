package com.kori.query.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BackofficeTransactionDetails(
        String transactionId,
        String type,
        String status,
        BigDecimal amount,
        String currency,
        String merchantCode,
        String agentCode,
        String clientId,
        String clientPhone,
        String merchantId,
        String agentId,
        String terminalUid,
        String cardUid,
        String originalTransactionId,
        BackofficePayoutInfo payout,
        BackofficeClientRefundInfo clientRefund,
        List<BackofficeLedgerLine> ledgerLines,
        Instant createdAt
) {
}
