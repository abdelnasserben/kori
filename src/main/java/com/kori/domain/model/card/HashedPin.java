package com.kori.domain.model.card;

import java.util.Objects;

public record HashedPin(String value) {
    public HashedPin {
        Objects.requireNonNull(value, "hashed pin must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("hashed pin must not be blank");
        }
    }
}
