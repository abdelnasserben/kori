package com.kori.domain.model.card;

import java.util.Objects;
import java.util.UUID;

public final class CardId {
    private final String value;

    private CardId(String value) {
        this.value = Objects.requireNonNull(value);
    }

    public static CardId newId() {
        return new CardId(UUID.randomUUID().toString());
    }

    public static CardId of(String value) {
        return new CardId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CardId other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
