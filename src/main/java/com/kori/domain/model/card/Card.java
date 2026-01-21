package com.kori.domain.model.card;

import com.kori.domain.model.account.AccountId;

import java.util.Objects;

public final class Card {
    private final CardId id;
    private final AccountId accountId;
    private final String cardUid;
    private final String pin;
    private final CardStatus status;
    private final int failedPinAttempts;

    public Card(CardId id,
                AccountId accountId,
                String cardUid,
                String pin,
                CardStatus status,
                int failedPinAttempts) {
        this.id = Objects.requireNonNull(id);
        this.accountId = Objects.requireNonNull(accountId);
        this.cardUid = Objects.requireNonNull(cardUid);
        this.pin = Objects.requireNonNull(pin);
        this.status = Objects.requireNonNull(status);
        if (failedPinAttempts < 0) {
            throw new IllegalArgumentException("failedPinAttempts must be >= 0");
        }
        this.failedPinAttempts = failedPinAttempts;
    }

    public static Card activeNew(AccountId accountId, String cardUid, String pin) {
        return new Card(CardId.newId(), accountId, cardUid, pin, CardStatus.ACTIVE, 0);
    }

    public CardId id() {
        return id;
    }

    public AccountId accountId() {
        return accountId;
    }

    public String cardUid() {
        return cardUid;
    }

    public String pin() {
        return pin;
    }

    public CardStatus status() {
        return status;
    }

    public int failedPinAttempts() {
        return failedPinAttempts;
    }

    public boolean isPayable() {
        return status == CardStatus.ACTIVE;
    }

    public Card onPinFailure(int maxAttempts) {
        int next = this.failedPinAttempts + 1;
        CardStatus nextStatus = (next >= maxAttempts) ? CardStatus.BLOCKED : this.status;
        return new Card(this.id, this.accountId, this.cardUid, this.pin, nextStatus, next);
    }

    public Card onPinSuccess() {
        if (this.failedPinAttempts == 0) return this;
        return new Card(this.id, this.accountId, this.cardUid, this.pin, this.status, 0);
    }
}
