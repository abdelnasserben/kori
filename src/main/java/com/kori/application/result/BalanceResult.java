package com.kori.application.result;

import com.kori.domain.ledger.LedgerAccount;

import java.math.BigDecimal;
import java.util.Objects;

public record BalanceResult(
        LedgerAccount ledgerAccount,
        String referenceId,
        BigDecimal balance
) {
    public BalanceResult {
        Objects.requireNonNull(ledgerAccount);
        Objects.requireNonNull(referenceId);
        Objects.requireNonNull(balance);
    }
}
