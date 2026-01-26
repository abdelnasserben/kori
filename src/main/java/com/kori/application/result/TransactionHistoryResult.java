package com.kori.application.result;

import com.kori.domain.ledger.LedgerAccountRef;

import java.time.Instant;
import java.util.List;

public record TransactionHistoryResult(
        LedgerAccountRef ledgerAccountRef,
        List<TransactionHistoryItem> items,

        // Next page cursor (stable)
        Instant nextBeforeCreatedAt,
        String nextBeforeTransactionId
) {}
