package com.kori.adapters.out.jpa.entity;

import com.kori.domain.ledger.LedgerAccountType;
import com.kori.domain.ledger.LedgerEntryType;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "ledger_entries",
        indexes = {
                @Index(name = "idx_ledger_tx", columnList = "transaction_id"),
                @Index(name = "idx_ledger_account", columnList = "account_type, owner_ref"),
                @Index(name = "idx_ledger_created_at", columnList = "created_at")
        }
)
@Access(AccessType.FIELD)
public class LedgerEntryEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, updatable = false, length = 32)
    private LedgerAccountType accountType;

    @Column(name = "owner_ref", nullable = false, updatable = false, length = 128)
    private String ownerRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, updatable = false, length = 16)
    private LedgerEntryType entryType;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected LedgerEntryEntity() {}

    public LedgerEntryEntity(
            UUID id,
            UUID transactionId,
            LedgerAccountType accountType,
            String ownerRef,
            LedgerEntryType entryType,
            BigDecimal amount
    ) {
        this.id = id;
        this.transactionId = transactionId;
        this.accountType = accountType;
        this.ownerRef = ownerRef;
        this.entryType = entryType;
        this.amount = amount;
    }
}
