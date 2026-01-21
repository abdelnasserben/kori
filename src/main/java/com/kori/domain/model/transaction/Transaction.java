package com.kori.domain.model.transaction;

import com.kori.domain.model.common.Money;

import java.time.Instant;
import java.util.Objects;

/**
 *
 * @param id
 * @param type
 * @param amount
 * @param createdAt
 * @param originalTransactionId // Reversal only: explicit link to the original transaction
 */
public record Transaction(
        TransactionId id,
        TransactionType type,
        Money amount,
        Instant createdAt,
        TransactionId originalTransactionId
    ) {

    public Transaction(TransactionId id, TransactionType type, Money amount, Instant createdAt, TransactionId originalTransactionId) {
        this.id = Objects.requireNonNull(id);
        this.type = Objects.requireNonNull(type);
        this.amount = Objects.requireNonNull(amount);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.originalTransactionId = originalTransactionId;
    }

    public static Transaction enrollCard(Money cardPrice, Instant createdAt) {
        return new Transaction(TransactionId.newId(), TransactionType.ENROLL_CARD, cardPrice, createdAt, null);
    }

    public static Transaction payByCard(Money amount, Instant createdAt) {
        return new Transaction(TransactionId.newId(), TransactionType.PAY_BY_CARD, amount, createdAt, null);
    }

    public static Transaction merchantWithdrawAtAgent(Money amount, Instant createdAt) {
        return new Transaction(TransactionId.newId(), TransactionType.MERCHANT_WITHDRAW_AT_AGENT, amount, createdAt, null);
    }

    public static Transaction agentPayout(Money amount, Instant createdAt) {
        return new Transaction(TransactionId.newId(), TransactionType.AGENT_PAYOUT, amount, createdAt, null);
    }

    public static Transaction reversal(TransactionId originalTransactionId, Money originalAmount, Instant createdAt) {
        return new Transaction(
                TransactionId.newId(),
                TransactionType.REVERSAL,
                originalAmount,
                createdAt,
                Objects.requireNonNull(originalTransactionId)
        );
    }
}
