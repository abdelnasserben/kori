package com.kori.application.port.out;

import com.kori.domain.ledger.LedgerAccount;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.common.Money;

import java.util.List;

public interface LedgerQueryPort {
    Money agentAvailableBalance(String agentId);

    /**
     * Returns the net balance for a given ledger scope: (sum(CREDIT) - sum(DEBIT)).
     *
     * <p>This is used for consultation features and for business pre-checks (ex: sufficient funds).
     */
    Money netBalance(LedgerAccount ledgerAccount, String referenceId);

    // Needed for generic reversal
    List<LedgerEntry> findByTransactionId(String transactionId);

    /**
     * Read-only access to the ledger for consultation features (balance & history).
     * Expected to return entries where {@code account == ledgerAccount} and {@code referenceId == referenceId}.
     */
    List<LedgerEntry> findEntries(LedgerAccount ledgerAccount, String referenceId);

}
