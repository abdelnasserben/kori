package com.kori.application.result;

import java.math.BigDecimal;
import java.util.Objects;

public record BalanceResult(
        String accountType,
        String ownerRef,
        BigDecimal balance
) {
    public BalanceResult {
        Objects.requireNonNull(accountType);
        Objects.requireNonNull(ownerRef);
        Objects.requireNonNull(balance);
    }
}
