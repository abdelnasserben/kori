package com.kori.domain.model.client;

import java.util.Objects;
import java.util.UUID;

public record ClientId(UUID value) {
    public ClientId(UUID value) {
        this.value = Objects.requireNonNull(value);
    }

    public static ClientId of(String value) {
        Objects.requireNonNull(value);
        return new ClientId(UUID.fromString(value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientId other)) return false;
        return value.equals(other.value);
    }

}
