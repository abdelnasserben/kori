package com.kori.domain.model.payout;

import com.kori.domain.model.agent.AgentId;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.TransactionId;

import java.time.Instant;
import java.util.Objects;

public final class Payout {
    private final PayoutId id;
    private final AgentId agentId;
    private final TransactionId transactionId;
    private final Money amount;
    private PayoutStatus status;
    private final Instant createdAt;
    private Instant completedAt;
    private Instant failedAt;
    private String failureReason;


    public Payout(PayoutId id,
                  AgentId agentId,
                  TransactionId transactionId,
                  Money amount,
                  PayoutStatus status,
                  Instant createdAt,
                  Instant completedAt,
                  Instant failedAt,
                  String failureReason) {
        this.id = Objects.requireNonNull(id);
        this.agentId = Objects.requireNonNull(agentId);
        this.transactionId = Objects.requireNonNull(transactionId);
        this.amount = Objects.requireNonNull(amount);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.completedAt = completedAt;
        this.failedAt = failedAt;
        this.failureReason = failureReason;
    }

    public static Payout requested(PayoutId payoutId, AgentId agentId, TransactionId transactionId, Money amount, Instant createdAt) {
        return new Payout(payoutId, agentId, transactionId, amount, PayoutStatus.REQUESTED, createdAt, null, null, null);
    }

    public boolean isFinal() {
        return status == PayoutStatus.COMPLETED || status == PayoutStatus.FAILED;
    }

    public PayoutId id() {
        return id;
    }

    public AgentId agentId() {
        return agentId;
    }

    public TransactionId transactionId() {
        return transactionId;
    }

    public Money amount() {
        return amount;
    }

    public PayoutStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public Instant failedAt() {
        return failedAt;
    }

    public String failureReason() {
        return failureReason;
    }

    public void complete(Instant at) {
        if (status != PayoutStatus.REQUESTED) throw new IllegalStateException("Can only complete REQUESTED payout");
        status = PayoutStatus.COMPLETED;
        completedAt = at;
    }

    public void fail(Instant at, String reason) {
        if (status != PayoutStatus.REQUESTED) throw new IllegalStateException("Can only fail REQUESTED payout");
        status = PayoutStatus.FAILED;
        failedAt = at;
        failureReason = reason;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Payout) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.agentId, that.agentId) &&
                Objects.equals(this.transactionId, that.transactionId) &&
                Objects.equals(this.amount, that.amount) &&
                Objects.equals(this.status, that.status) &&
                Objects.equals(this.createdAt, that.createdAt) &&
                Objects.equals(this.completedAt, that.completedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, agentId, transactionId, amount, status, createdAt, completedAt);
    }

    @Override
    public String toString() {
        return "Payout[" +
                "id=" + id + ", " +
                "agentCode=" + agentId + ", " +
                "transactionId=" + transactionId + ", " +
                "amount=" + amount + ", " +
                "status=" + status + ", " +
                "createdAt=" + createdAt + ", " +
                "completedAt=" + completedAt + ']';
    }

}
