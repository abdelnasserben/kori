package com.kori.application.result;

import java.util.Objects;

public record AgentUpdateCardStatusResult(String cardId, String cardUid, String status) {

    public AgentUpdateCardStatusResult(String cardId, String cardUid, String status) {
        this.cardId = Objects.requireNonNull(cardId);
        this.cardUid = Objects.requireNonNull(cardUid);
        this.status = Objects.requireNonNull(status);
    }
}
