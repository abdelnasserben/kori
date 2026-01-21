package com.kori.application.result;

import java.util.Objects;

public record AdminUnblockCardResult(String cardId, String cardUid, String status, int failedPinAttempts) {

    public AdminUnblockCardResult(String cardId, String cardUid, String status, int failedPinAttempts) {
        this.cardId = Objects.requireNonNull(cardId);
        this.cardUid = Objects.requireNonNull(cardUid);
        this.status = Objects.requireNonNull(status);
        this.failedPinAttempts = failedPinAttempts;
    }
}
