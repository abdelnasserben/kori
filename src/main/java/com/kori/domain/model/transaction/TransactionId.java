package com.kori.domain.model.transaction;

import java.util.Objects;
import java.util.UUID;

public final class TransactionId {
    private final String value;

    private TransactionId(String value) {
        this.value = Objects.requireNonNull(value);
    }

    public static TransactionId newId() {
        return new TransactionId(UUID.randomUUID().toString());
    }

    public static TransactionId of(String value) {
        return new TransactionId(value);
    }

    public String value() {
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
