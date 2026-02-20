package com.kori.application.port.out;

import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.TransactionId;
import com.kori.domain.model.transaction.TransactionType;

import java.util.List;

public interface LedgerQueryPort {

    /**
     * Returns the net balance for a given ledger accountRef ref: (sum(CREDIT) - sum(DEBIT)).
     */
    Money netBalance(LedgerAccountRef account);

    /**
     * Alias explicite utilisé par les flux métier qui manipulent des limites.
     */
    Money getBalance(LedgerAccountRef account);

    // Needed for generic reversal
    List<LedgerEntry> findByTransactionId(TransactionId transactionId);

    /**
     * Read-only access to the ledger for consultation features (balance & history).
     * Expected to return entries where {@code entry.accountRef() == accountRef}.
     */
    List<LedgerEntry> findEntries(LedgerAccountRef account);

    /**
     * Returns transaction ids that violate double-entry invariants
     * (missing debit/credit or unbalanced amounts).
     */
    List<TransactionId> findInconsistentTransactionIds();

    /**
     * Returns the sum of debits for a given account, transaction type and period.
     * Used for limit checks.
     */
    Money sumDebitsByTransactionTypeAndPeriod(
            LedgerAccountRef account,
            TransactionType transactionType,
            java.time.Instant fromInclusive,
            java.time.Instant toExclusive
    );
}
