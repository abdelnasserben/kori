package com.kori.domain.model.transaction;

import java.util.Objects;
import java.util.UUID;

public final class TransactionId {
    private final UUID value;

    private TransactionId(UUID value) {
        this.value = Objects.requireNonNull(value);
    }

    public static TransactionId newId() {
        return new TransactionId(UUID.randomUUID());
    }

    public static TransactionId of(String value) {
        return new TransactionId(UUID.fromString(value));
    }

    public UUID value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionId other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
