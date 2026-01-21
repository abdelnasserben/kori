package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Entity
@Table(name = "cards")
@Access(AccessType.FIELD)
public class CardEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "card_uid", nullable = false, unique = true, length = 128)
    private String cardUid;

    @Setter
    @Column(name = "pin", nullable = false, length = 64)
    private String pin;

    @Setter
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Setter
    @Column(name = "failed_pin_attempts", nullable = false)
    private int failedPinAttempts;

    protected CardEntity() { }

    public CardEntity(UUID id, UUID accountId, String cardUid, String pin, String status, int failedPinAttempts) {
        this.id = id;
        this.accountId = accountId;
        this.cardUid = cardUid;
        this.pin = pin;
        this.status = status;
        this.failedPinAttempts = failedPinAttempts;
    }

}
