package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "client_refunds", indexes = {
        @Index(name = "idx_client_refunds_client", columnList = "client_id"),
        @Index(name = "idx_client_refunds_tx", columnList = "transaction_id", unique = true),
        @Index(name = "idx_client_refunds_status", columnList = "status")
})
@Access(AccessType.FIELD)
public class ClientRefundEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Column(name = "transaction_id", nullable = false, updatable = false, unique = true)
    private UUID transactionId;

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    protected ClientRefundEntity() {}

    public ClientRefundEntity(UUID id, UUID clientId, UUID transactionId, BigDecimal amount, String status,
                              Instant createdAt, Instant completedAt, Instant failedAt, String failureReason) {
        this.id = id;
        this.clientId = clientId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.failedAt = failedAt;
        this.failureReason = failureReason;
    }
}
