package com.kori.query.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BackofficeTransactionDetails(
        String transactionRef,
        String type,
        String status,
        BigDecimal amount,
        String currency,
        String merchantCode,
        String agentCode,
        String clientCode,
        String clientPhone,
        String terminalUid,
        String cardUid,
        String originalTransactionRef,
        BackofficePayoutInfo payout,
        BackofficeClientRefundInfo clientRefund,
        List<BackofficeLedgerLine> ledgerLines,
        Instant createdAt
) {
}
