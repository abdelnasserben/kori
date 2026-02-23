package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "admins",
        indexes = {
                @Index(name = "idx_admin_status", columnList = "status")
        }
)
@Access(AccessType.FIELD)
public class AdminEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false, length = 16, unique = true)
    private String username;

    @Column(name = "display_name", length = 120)
    private String displayName;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AdminEntity() {
        // for JPA
    }

    public AdminEntity(UUID id, String username, String displayName, String status, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.status = status;
        this.createdAt = createdAt;
    }
}
