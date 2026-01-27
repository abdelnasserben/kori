package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;

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

    @Column(name = "merchant_id", nullable = false, updatable = false, length = 36)
    private UUID merchantId;

    @Column(nullable = false, length = 16)
    private String status;

    protected TerminalEntity() {}

    public TerminalEntity(UUID id, UUID merchantId, String status) {
        this.id = id;
        this.merchantId = merchantId;
        this.status = status;
    }
}
