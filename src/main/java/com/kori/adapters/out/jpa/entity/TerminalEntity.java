package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "terminals")
@Access(AccessType.FIELD)
public class TerminalEntity {
    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "merchant_id", nullable = false, length = 64)
    private String merchantId;

    protected TerminalEntity() { }
    public TerminalEntity(String id, String merchantId) { this.id = id; this.merchantId = merchantId; }

    public String getId() { return id; }
    public String getMerchantId() { return merchantId; }
}
