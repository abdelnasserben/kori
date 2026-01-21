package com.kori.application.result;

import com.kori.domain.model.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionHistoryItem(
        String transactionId,
        TransactionType transactionType,
        Instant createdAt,

        // Counterparties (when present in ledger entries of the tx)
        String clientId,
        String merchantId,
        String agentId,

        // Generic self-scope aggregates (for SUMMARY)
        BigDecimal selfTotalDebits,
        BigDecimal selfTotalCredits,
        BigDecimal selfNet,

        // PAY_BY_CARD_VIEW projection
        BigDecimal amount,        // merchant credit
        BigDecimal fee,           // platform credit
        BigDecimal totalDebited   // client debit
) {}
