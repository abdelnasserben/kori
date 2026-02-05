package com.kori.domain.ledger;

public enum LedgerAccountType {
    CLIENT,
    MERCHANT,
    AGENT_WALLET,
    AGENT_CASH_CLEARING,

    PLATFORM_FEE_REVENUE,
    PLATFORM_CLEARING,
    PLATFORM_CLIENT_REFUND_CLEARING,
    PLATFORM_BANK
}
