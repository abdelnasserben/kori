package com.kori.domain.model.card;

import com.kori.domain.common.InvalidStatusTransitionException;
import com.kori.domain.model.client.ClientId;

import java.util.Objects;

public final class Card {

    private final CardId id;
    private final ClientId clientId;
    private final String cardUid;
    private final HashedPin hashedPin;

    private CardStatus status;
    private int failedPinAttempts;

    public Card(
            CardId id,
            ClientId clientId,
            String cardUid,
            HashedPin hashedPin,
            CardStatus status,
            int failedPinAttempts
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.clientId = Objects.requireNonNull(clientId, "cardUid");
        this.cardUid = Objects.requireNonNull(cardUid, "cardUid");
        this.hashedPin = Objects.requireNonNull(hashedPin, "hashedPin");
        this.status = Objects.requireNonNull(status, "status");
        if (failedPinAttempts < 0) {
            throw new IllegalArgumentException("failedPinAttempts must be >= 0");
        }
        this.failedPinAttempts = failedPinAttempts;
    }

    public static Card activeNew(ClientId clientId, String cardUid, HashedPin pin) {
        return new Card(CardId.newId(), clientId, cardUid, pin, CardStatus.ACTIVE, 0);
    }

    public CardId id() {
        return id;
    }

    public ClientId clientId() {
        return clientId;
    }

    public String cardUid() {
        return cardUid;
    }

    public HashedPin hashedPin() {
        return hashedPin;
    }

    public CardStatus status() {
        return status;
    }

    public int failedPinAttempts() {
        return failedPinAttempts;
    }

    public boolean isPayable() {
        return this.status == CardStatus.ACTIVE;
    }

    public boolean hasFailedPinAttempts() {
        return failedPinAttempts > 0;
    }

    // -----------------
    // PIN attempts rules
    // -----------------

    public void onPinSuccess() {
        if (failedPinAttempts > 0) {
            failedPinAttempts = 0;
        }
    }

    public void onPinFailure(int maxAttempts) {
        if (maxAttempts <= 0) throw new IllegalArgumentException("maxAttempts must be > 0");
        if (status == CardStatus.LOST) return; // LOST is terminal

        failedPinAttempts++;

        if (failedPinAttempts >= maxAttempts) {
            // auto-block
            status = CardStatus.BLOCKED;
        }
    }

    // -----------------
    // Explicit status actions
    // -----------------

    /** Agent action: terminal state */
    public void markLost() {
        if (status == CardStatus.LOST) return; // idempotent
        status = CardStatus.LOST;
    }

    /** Agent/system action */
    public void block() {
        ensureNotLost();
        if (status == CardStatus.BLOCKED) return;
        status = CardStatus.BLOCKED;
    }

    /** Admin-only action */
    public void suspend() {
        ensureNotLost();
        if (status == CardStatus.SUSPENDED) return;
        status = CardStatus.SUSPENDED;
    }

    /** Admin-only action */
    public void deactivate() {
        ensureNotLost();
        if (status == CardStatus.INACTIVE) return;
        status = CardStatus.INACTIVE;
    }

    /** Admin-only action */
    public void activate() {
        ensureNotLost();
        if (status == CardStatus.BLOCKED) {
            throw new InvalidStatusTransitionException("BLOCKED -> ACTIVE requires unblock");
        }
        status = CardStatus.ACTIVE;
    }

    /** Admin-only special path: resets failed PIN attempts */
    public void unblock() {
        if (status != CardStatus.BLOCKED) {
            throw new InvalidStatusTransitionException("Only BLOCKED cards can be unblocked");
        }
        status = CardStatus.ACTIVE;
        failedPinAttempts = 0;
    }

    private void ensureNotLost() {
        if (status == CardStatus.LOST) {
            throw new InvalidStatusTransitionException("LOST card is terminal and cannot change status");
        }
    }
}
