package com.kori.application.port.out;

import com.kori.domain.ledger.LedgerAccountRef;

/**
 * Locks a ledger account scope to prevent concurrent balance checks and debits.
 */
public interface LedgerAccountLockPort {

    void lock(LedgerAccountRef accountRef);
}
