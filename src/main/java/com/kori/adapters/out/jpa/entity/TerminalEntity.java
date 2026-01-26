package com.kori.adapters.out.jpa.entity;

import com.kori.domain.model.common.Status;
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
    private String merchantId; // MerchantId.value()

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    protected TerminalEntity() {}

    public TerminalEntity(UUID id, String merchantId, Status status) {
        this.id = id;
        this.merchantId = merchantId;
        this.status = status;
    }
}
