package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Entity
@Table(name = "accounts")
@Access(AccessType.FIELD)
public class AccountEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "client_id", nullable = false, unique = true)
    private UUID clientId;

    @Setter
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    protected AccountEntity() { }

    public AccountEntity(UUID id, UUID clientId, String status) {
        this.id = id;
        this.clientId = clientId;
        this.status = status;
    }

}
