package com.kori.query.model;

import java.math.BigDecimal;

public record BackofficeLedgerLine(
        String accountType,
        String ownerRef,
        String entryType,
        BigDecimal amount,
        String currency
) {
}
