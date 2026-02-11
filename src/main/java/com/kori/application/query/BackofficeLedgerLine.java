package com.kori.application.query;

import java.math.BigDecimal;

public record BackofficeLedgerLine(
        String accountType,
        String ownerRef,
        String entryType,
        BigDecimal amount,
        String currency
) {
}
