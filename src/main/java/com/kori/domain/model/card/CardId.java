package com.kori.domain.model.card;

import java.util.Objects;
import java.util.UUID;

public record CardId(UUID value) {
    public CardId(UUID value) {
        this.value = Objects.requireNonNull(value);
    }

    public static CardId newId() {
        return new CardId(UUID.randomUUID());
    }

    public static CardId of(String value) {
        return new CardId(UUID.fromString(value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CardId other)) return false;
        return value.equals(other.value);
    }

}
