package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "idempotency_records")
@Access(AccessType.FIELD)
public class IdempotencyRecordEntity {

    @Getter
    @Id
    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Getter
    @Column(name = "result_type", nullable = false, length = 256)
    private String resultType;

    @Getter
    @Column(name = "result_json", nullable = false, columnDefinition = "text")
    private String resultJson;

    @Getter
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Getter
    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected IdempotencyRecordEntity() { }

    public IdempotencyRecordEntity(String idempotencyKey, String resultType, String resultJson, String requestHash, OffsetDateTime expiresAt) {
        this.idempotencyKey = idempotencyKey;
        this.resultType = resultType;
        this.resultJson = resultJson;
        this.requestHash = requestHash;
        this.expiresAt = expiresAt;
    }

}
