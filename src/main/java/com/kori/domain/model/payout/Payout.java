package com.kori.domain.model.payout;

import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.TransactionId;

import java.time.Instant;
import java.util.Objects;

public record Payout(
        PayoutId id,
        String agentId,
        TransactionId transactionId,
        Money amount,
        PayoutStatus status,
        Instant createdAt,
        Instant completedAt
) {

    public Payout(PayoutId id,
                  String agentId,
                  TransactionId transactionId,
                  Money amount,
                  PayoutStatus status,
                  Instant createdAt,
                  Instant completedAt) {
        this.id = Objects.requireNonNull(id);
        this.agentId = Objects.requireNonNull(agentId);
        this.transactionId = Objects.requireNonNull(transactionId);
        this.amount = Objects.requireNonNull(amount);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.completedAt = completedAt;
    }

    public static Payout requested(String agentId, TransactionId transactionId, Money amount, Instant createdAt) {
        return new Payout(PayoutId.newId(), agentId, transactionId, amount, PayoutStatus.REQUESTED, createdAt, null);
    }

    public Payout complete(Instant completedAt) {
        return new Payout(
                this.id,
                this.agentId,
                this.transactionId,
                this.amount,
                PayoutStatus.COMPLETED,
                this.createdAt,
                Objects.requireNonNull(completedAt)
        );
    }
}
