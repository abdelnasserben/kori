package com.kori.domain.model.transaction;

import java.util.Objects;
import java.util.UUID;

public record TransactionId(UUID value) {
    public TransactionId(UUID value) {
        this.value = Objects.requireNonNull(value);
    }

    public static TransactionId of(String value) {
        return new TransactionId(UUID.fromString(value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionId other)) return false;
        return value.equals(other.value);
    }

}
