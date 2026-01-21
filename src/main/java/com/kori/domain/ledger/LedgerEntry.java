package com.kori.domain.ledger;

import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.TransactionId;

import java.util.Objects;
import java.util.UUID;

/**
 * @param referenceId ex: agentId, merchantId, clientId
 */
public record LedgerEntry(String id, TransactionId transactionId, LedgerAccount account, LedgerEntryType type,
                          Money amount, String referenceId) {
    public LedgerEntry(String id,
                       TransactionId transactionId,
                       LedgerAccount account,
                       LedgerEntryType type,
                       Money amount,
                       String referenceId) {
        this.id = Objects.requireNonNull(id);
        this.transactionId = Objects.requireNonNull(transactionId);
        this.account = Objects.requireNonNull(account);
        this.type = Objects.requireNonNull(type);
        this.amount = Objects.requireNonNull(amount);
        this.referenceId = referenceId;
    }

    public static LedgerEntry credit(TransactionId txId, LedgerAccount account, Money amount, String referenceId) {
        return new LedgerEntry(UUID.randomUUID().toString(), txId, account, LedgerEntryType.CREDIT, amount, referenceId);
    }

    public static LedgerEntry debit(TransactionId txId, LedgerAccount account, Money amount, String referenceId) {
        return new LedgerEntry(UUID.randomUUID().toString(), txId, account, LedgerEntryType.DEBIT, amount, referenceId);
    }
}
