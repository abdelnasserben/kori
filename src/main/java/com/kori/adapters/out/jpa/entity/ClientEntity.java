package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "clients")
@Access(AccessType.FIELD)
public class ClientEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "phone_number", nullable = false, unique = true, length = 32)
    private String phoneNumber;

    @Setter
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ClientEntity() { }

    public ClientEntity(UUID id, String phoneNumber, String status, OffsetDateTime createdAt) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.status = status;
        this.createdAt = createdAt;
    }

}
