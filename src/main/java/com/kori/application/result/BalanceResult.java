package com.kori.application.result;

import com.kori.domain.ledger.LedgerAccountRef;

import java.math.BigDecimal;
import java.util.Objects;

public record BalanceResult(
        LedgerAccountRef ledgerAccountRef,
        BigDecimal balance
) {
    public BalanceResult {
        Objects.requireNonNull(ledgerAccountRef);
        Objects.requireNonNull(balance);
    }
}
