package com.kori.adapters.out.jpa.entity;

import com.kori.domain.model.common.Status;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "agents",
        indexes = {
                @Index(name = "idx_agent_code", columnList = "code", unique = true),
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AgentEntity() {}

    public AgentEntity(UUID id, String code, Status status, Instant createdAt) {
        this.id = id;
        this.code = code;
        this.status = status;
        this.createdAt = createdAt;
    }
}
