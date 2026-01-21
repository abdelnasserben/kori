package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "merchants")
@Access(AccessType.FIELD)
public class MerchantEntity {
    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    protected MerchantEntity() { }
    public MerchantEntity(String id, String status) { this.id = id; this.status = status; }

    public String getId() { return id; }
    public String getStatus() { return status; }
}
