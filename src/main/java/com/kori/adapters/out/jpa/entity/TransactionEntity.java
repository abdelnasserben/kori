package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "transactions",
        indexes = {
                @Index(name = "idx_transactions_created_at", columnList = "created_at"),
                @Index(name = "idx_transactions_original_tx", columnList = "original_transaction_id")
        })
@Access(AccessType.FIELD)
public class TransactionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "type", nullable = false, length = 64)
    private String type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "original_transaction_id")
    private UUID originalTransactionId;

    protected TransactionEntity() { }

    public TransactionEntity(UUID id, String type, BigDecimal amount, OffsetDateTime createdAt, UUID originalTransactionId) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.createdAt = createdAt;
        this.originalTransactionId = originalTransactionId;
    }

}
