package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "terminals",
        indexes = {
                @Index(name = "idx_terminal_merchant", columnList = "merchant_id"),
                @Index(name = "idx_terminal_status", columnList = "status")
        }
)
@Access(AccessType.FIELD)
public class TerminalEntity {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private UUID id;

    @Column(nullable = false, updatable = false, length = 36, unique = true)
    private String terminalUid;

    @Column(name = "merchant_id", nullable = false, updatable = false, length = 36)
    private UUID merchantId;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TerminalEntity() {}

    public TerminalEntity(UUID id, String terminalUid, UUID merchantId, String status, Instant createdAt) {
        this.id = id;
        this.terminalUid = terminalUid;
        this.merchantId = merchantId;
        this.status = status;
        this.createdAt = createdAt;
    }
}
