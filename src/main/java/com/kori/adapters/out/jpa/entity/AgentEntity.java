package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "agents",
        indexes = {
                @Index(name = "idx_agent_code", columnList = "code"),
                @Index(name = "idx_agent_status", columnList = "status")
        }
)
@Access(AccessType.FIELD)
public class AgentEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false, length = 16, unique = true)
    private String code;

    @Column(name = "display_name", length = 120)
    private String displayName;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AgentEntity() {
        // for JPA
    }

    public AgentEntity(UUID id, String code, String displayName, String status, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");

        String normalizedCode = Objects.requireNonNull(code, "code").trim();
        if (normalizedCode.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        this.code = normalizedCode;

        this.displayName = displayName;
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }
}
