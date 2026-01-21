package com.kori.application.command;

public enum TransactionHistoryView {
    /**
     * Generic per-transaction summary for the actor's own ledger:
     * totals (debits/credits/net) for the scope ledger.
     */
    SUMMARY,

    /**
     * UI-friendly view for card payments (PAY_BY_CARD).
     * Builds: amount (merchant credit), fee (platform credit), totalDebited (client debit),
     * and counterparties (clientId/merchantId/agentId when present).
     */
    PAY_BY_CARD_VIEW,

    /**
     * UI-friendly view for commissions (where AGENT has credits).
     */
    COMMISSION_VIEW
}
