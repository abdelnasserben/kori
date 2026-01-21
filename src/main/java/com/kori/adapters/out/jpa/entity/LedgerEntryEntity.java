package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "ledger_entries",
        indexes = {
                @Index(name = "idx_ledger_tx", columnList = "transaction_id"),
                @Index(name = "idx_ledger_account_ref", columnList = "account, reference_id"),
                @Index(name = "idx_ledger_created_at", columnList = "created_at")
        })
@Access(AccessType.FIELD)
public class LedgerEntryEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @Column(name = "account", nullable = false, updatable = false, length = 64)
    private String account;

    @Column(name = "entry_type", nullable = false, updatable = false, length = 16)
    private String entryType;

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "reference_id", updatable = false, length = 128)
    private String referenceId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected LedgerEntryEntity() { }

    public LedgerEntryEntity(UUID id, UUID transactionId, String account, String entryType, BigDecimal amount, String referenceId) {
        this.id = id;
        this.transactionId = transactionId;
        this.account = account;
        this.entryType = entryType;
        this.amount = amount;
        this.referenceId = referenceId;
    }

}
