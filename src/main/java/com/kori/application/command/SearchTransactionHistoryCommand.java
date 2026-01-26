package com.kori.application.command;

import com.kori.application.security.ActorContext;
import com.kori.domain.ledger.LedgerAccount;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record SearchTransactionHistoryCommand(
        ActorContext actorContext,

        // Optional: ADMIN can query an arbitrary scope; others must query self.
        LedgerAccountRef ledgerAccountRef,

        // Filters
        TransactionType transactionType,
        Instant from,
        Instant to,

        // Cursor-based pagination (stable)
        Instant beforeCreatedAt,
        String beforeTransactionId,

        // Amount filters (inclusive)
        BigDecimal minAmount,
        BigDecimal maxAmount,

        // View (projection)
        TransactionHistoryView view,

        // Page size
        int limit
) {
    public SearchTransactionHistoryCommand {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        if (limit <= 0) limit = 50;
        if (view == null) view = TransactionHistoryView.SUMMARY;

        // Cursor must be fully defined or fully null
        if ((beforeCreatedAt == null) != (beforeTransactionId == null)) {
            throw new IllegalArgumentException("beforeCreatedAt and beforeTransactionId must be both null or both non-null");
        }

        if (minAmount != null && minAmount.signum() < 0) {
            throw new IllegalArgumentException("minAmount must be >= 0");
        }
        if (maxAmount != null && maxAmount.signum() < 0) {
            throw new IllegalArgumentException("maxAmount must be >= 0");
        }

        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("minAmount must be <= maxAmount");
        }
    }
}
