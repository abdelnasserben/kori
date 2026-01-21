package com.kori.application.result;

import com.kori.domain.ledger.LedgerAccount;

import java.time.Instant;
import java.util.List;

public record TransactionHistoryResult(
        LedgerAccount ledgerAccount,
        String referenceId,
        List<TransactionHistoryItem> items,

        // Next page cursor (stable)
        Instant nextBeforeCreatedAt,
        String nextBeforeTransactionId
) {}
