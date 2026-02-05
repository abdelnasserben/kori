package com.kori.domain.model.clientrefund;

import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.TransactionId;

import java.time.Instant;
import java.util.Objects;

public final class ClientRefund {
    private final ClientRefundId id;
    private final ClientId clientId;
    private final TransactionId transactionId;
    private final Money amount;
    private ClientRefundStatus status;
    private final Instant createdAt;
    private Instant completedAt;
    private Instant failedAt;
    private String failureReason;

    public ClientRefund(ClientRefundId id,
                        ClientId clientId,
                        TransactionId transactionId,
                        Money amount,
                        ClientRefundStatus status,
                        Instant createdAt,
                        Instant completedAt,
                        Instant failedAt,
                        String failureReason) {
        this.id = Objects.requireNonNull(id);
        this.clientId = Objects.requireNonNull(clientId);
        this.transactionId = Objects.requireNonNull(transactionId);
        this.amount = Objects.requireNonNull(amount);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.completedAt = completedAt;
        this.failedAt = failedAt;
        this.failureReason = failureReason;
    }

    public static ClientRefund requested(ClientRefundId id, ClientId clientId, TransactionId transactionId, Money amount, Instant createdAt) {
        return new ClientRefund(id, clientId, transactionId, amount, ClientRefundStatus.REQUESTED, createdAt, null, null, null);
    }

    public void complete(Instant at) {
        if (status != ClientRefundStatus.REQUESTED) throw new IllegalStateException("Can only complete REQUESTED client refund");
        status = ClientRefundStatus.COMPLETED;
        completedAt = at;
    }

    public void fail(Instant at, String reason) {
        if (status != ClientRefundStatus.REQUESTED) throw new IllegalStateException("Can only fail REQUESTED client refund");
        status = ClientRefundStatus.FAILED;
        failedAt = at;
        failureReason = reason;
    }

    public ClientRefundId id() { return id; }
    public ClientId clientId() { return clientId; }
    public TransactionId transactionId() { return transactionId; }
    public Money amount() { return amount; }
    public ClientRefundStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant completedAt() { return completedAt; }
    public Instant failedAt() { return failedAt; }
    public String failureReason() { return failureReason; }
}
