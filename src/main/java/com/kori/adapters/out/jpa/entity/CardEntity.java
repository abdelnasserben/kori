package com.kori.adapters.out.jpa.entity;

import com.kori.domain.model.card.CardStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Getter
@Entity
@Table(
        name = "cards",
        indexes = {
                @Index(name = "idx_cards_client_id", columnList = "client_id"),
                @Index(name = "idx_cards_uid", columnList = "card_uid", unique = true),
                @Index(name = "idx_cards_status", columnList = "status")
        }
)
@Access(AccessType.FIELD)
public class CardEntity {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private UUID id;

    @Column(name = "client_id", nullable = false, updatable = false, length = 64)
    private UUID clientId;

    @Column(name = "card_uid", nullable = false, updatable = false, length = 64, unique = true)
    private String cardUid;

    @Column(name = "hashed_pin", nullable = false, length = 255)
    private String hashedPin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CardStatus status;

    @Column(name = "failed_pin_attempts", nullable = false)
    private int failedPinAttempts;

    protected CardEntity() {}

    public CardEntity(
            UUID id,
            UUID clientId,
            String cardUid,
            String hashedPin,
            CardStatus status,
            int failedPinAttempts
    ) {
        this.id = id;
        this.clientId = clientId;
        this.cardUid = cardUid;
        this.hashedPin = hashedPin;
        this.status = status;
        this.failedPinAttempts = failedPinAttempts;
    }
}
