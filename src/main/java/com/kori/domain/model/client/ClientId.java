package com.kori.domain.model.client;

import java.util.Objects;
import java.util.UUID;

public final class ClientId {
    private final String value;

    private ClientId(String value) {
        this.value = Objects.requireNonNull(value);
    }

    public static ClientId newId() {
        return new ClientId(UUID.randomUUID().toString());
    }

    public static ClientId of(String value) {
        return new ClientId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientId other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
