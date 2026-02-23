package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "merchants",
        indexes = {
                @Index(name = "idx_merchant_code", columnList = "code", unique = true),
                @Index(name = "idx_merchant_status", columnList = "status")
        }
)
@Access(AccessType.FIELD)
public class MerchantEntity {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private UUID id;

    @Column(nullable = false, updatable = false, length = 16, unique = true)
    private String code;

    @Column(name = "display_name", length = 120)
    private String displayName;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MerchantEntity() {}

    public MerchantEntity(UUID id, String code, String displayName, String status, Instant createdAt) {
        this.id = id;
        this.code = code;
        this.displayName = displayName;
        this.status = status;
        this.createdAt = createdAt;
    }
}
