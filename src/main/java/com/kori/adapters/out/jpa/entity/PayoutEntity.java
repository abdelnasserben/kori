package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "payouts",
        indexes = {
                @Index(name = "idx_payouts_agent", columnList = "agent_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payouts_transaction_id", columnNames = {"transaction_id"})
        })
@Access(AccessType.FIELD)
public class PayoutEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "agent_id", nullable = false, length = 64)
    private String agentId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Setter
    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Setter
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected PayoutEntity() { }

    public PayoutEntity(UUID id,
                        String agentId,
                        UUID transactionId,
                        BigDecimal amount,
                        String status,
                        OffsetDateTime createdAt,
                        OffsetDateTime completedAt) {
        this.id = id;
        this.agentId = agentId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

}
