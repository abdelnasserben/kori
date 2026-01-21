package com.kori.domain.model.payout;

import java.util.Objects;
import java.util.UUID;

public record PayoutId(UUID value) {

    public PayoutId {
        Objects.requireNonNull(value);
    }

    public static PayoutId newId() {
        return new PayoutId(UUID.randomUUID());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PayoutId other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
