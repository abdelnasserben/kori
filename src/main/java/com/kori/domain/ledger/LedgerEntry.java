package com.kori.domain.ledger;

import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.TransactionId;

import java.util.Objects;
import java.util.UUID;

public record LedgerEntry(
        String id,
        TransactionId transactionId,
        LedgerAccountRef accountRef,
        LedgerEntryType type,
        Money amount
) {
    public LedgerEntry(String id,
                       TransactionId transactionId,
                       LedgerAccountRef accountRef,
                       LedgerEntryType type,
                       Money amount) {
        this.id = Objects.requireNonNull(id, "id");
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId");
        this.accountRef = Objects.requireNonNull(accountRef, "accountRef");
        this.type = Objects.requireNonNull(type, "type");
        this.amount = Objects.requireNonNull(amount, "amount");
    }

    public static LedgerEntry credit(TransactionId txId, LedgerAccountRef account, Money amount) {
        return new LedgerEntry(UUID.randomUUID().toString(), txId, account, LedgerEntryType.CREDIT, amount);
    }

    public static LedgerEntry debit(TransactionId txId, LedgerAccountRef account, Money amount) {
        return new LedgerEntry(UUID.randomUUID().toString(), txId, account, LedgerEntryType.DEBIT, amount);
    }
}
