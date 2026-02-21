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

        if(type == TransactionType.REVERSAL) {
            this.originalTransactionId = Objects.requireNonNull(originalTransactionId, "original transaction ID is required for REVERSAL");
        } else {
            if (originalTransactionId != null) {
                throw new IllegalArgumentException("original transaction ID must be null for non-REVERSAL transactions");
            }
            this.originalTransactionId = null;
        }
    }

    public static Transaction enrollCard(TransactionId id, Money cardPrice, Instant createdAt) {
        return new Transaction(id, TransactionType.ENROLL_CARD, cardPrice, createdAt, null);
    }

    public static Transaction payByCard(TransactionId id, Money amount, Instant createdAt) {
        return new Transaction(id, TransactionType.PAY_BY_CARD, amount, createdAt, null);
    }

    public static Transaction merchantWithdrawAtAgent(TransactionId id, Money amount, Instant createdAt) {
        return new Transaction(id, TransactionType.MERCHANT_WITHDRAW_AT_AGENT, amount, createdAt, null);
    }

    public static Transaction agentPayout(TransactionId id, Money amount, Instant createdAt) {
        return new Transaction(id, TransactionType.AGENT_PAYOUT, amount, createdAt, null);
    }

    public static Transaction agentBankDepositReceipt(TransactionId id, Money amount, Instant createdAt) {
        return new Transaction(id, TransactionType.AGENT_BANK_DEPOSIT_RECEIPT, amount, createdAt, null);
    }

    public static Transaction reversal(TransactionId id, TransactionId originalTransactionId, Money originalAmount, Instant createdAt) {
        return new Transaction(
                id,
                TransactionType.REVERSAL,
                originalAmount,
                createdAt,
                originalTransactionId
        );
    }

    public static Transaction cashInByAgent(TransactionId id, Money amount, Instant createdAt) {
        return new Transaction(id, TransactionType.CASH_IN_BY_AGENT, amount, createdAt, null);
    }

    public static Transaction clientRefund(TransactionId id, Money amount, Instant createdAt) {
        return new Transaction(id, TransactionType.CLIENT_REFUND, amount, createdAt, null);
    }

    public static Transaction clientTransfer(TransactionId id, Money amount, Instant createdAt) {
        return new Transaction(id, TransactionType.CLIENT_TRANSFER, amount, createdAt, null);
    }

    public static Transaction merchantTransfer(TransactionId id, Money amount, Instant createdAt) {
        return new Transaction(id, TransactionType.MERCHANT_TRANSFER, amount, createdAt, null);
    }
}
