package com.kori.domain.model.card;

import com.kori.domain.model.account.AccountId;

import java.util.Objects;

public record Card(
        CardId id,
        AccountId accountId,
        String cardUid,
        HashedPin hashedPin,
        CardStatus status,
        int failedPinAttempts) {

    public Card(CardId id,
                AccountId accountId,
                String cardUid,
                HashedPin hashedPin,
                CardStatus status,
                int failedPinAttempts) {
        this.id = Objects.requireNonNull(id);
        this.accountId = Objects.requireNonNull(accountId);
        this.cardUid = Objects.requireNonNull(cardUid);
        this.hashedPin = Objects.requireNonNull(hashedPin);
        this.status = Objects.requireNonNull(status);
        this.failedPinAttempts = failedPinAttempts;
    }

    public static Card activeNew(AccountId accountId, String cardUid, HashedPin pin) {
        return new Card(CardId.newId(), accountId, cardUid, pin, CardStatus.ACTIVE, 0);
    }

    public boolean isPayable() {
        return status.isPayable();
    }

    public boolean hasFailedPinAttempts() {
        return failedPinAttempts > 0;
    }

    public Card onPinSuccess() {
        if (failedPinAttempts == 0) return this;
        return new Card(id, accountId, cardUid, hashedPin, status, 0);
    }

    public Card onPinFailure(int maxAttempts) {
        int next = failedPinAttempts + 1;
        if (next >= maxAttempts) {
            // auto-block
            return new Card(id, accountId, cardUid, hashedPin, CardStatus.BLOCKED, next);
        }
        return new Card(id, accountId, cardUid, hashedPin, status, next);
    }

    /**
     * Generic status transition, guarded by CardStatus matrix.
     */
    public Card transitionTo(CardStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidCardStatusTransitionException(
                    "Invalid card status transition: " + status + " -> " + target
            );
        }
        return new Card(id, accountId, cardUid, hashedPin, target, failedPinAttempts);
    }

    /**
     * Unblock is special because it resets failed PIN attempts.
     */
    public Card unblockToActive() {
        if (status != CardStatus.BLOCKED) {
            throw new InvalidCardStatusTransitionException("Only BLOCKED cards can be unblocked");
        }
        return new Card(id, accountId, cardUid, hashedPin, CardStatus.ACTIVE, 0);
    }
}
