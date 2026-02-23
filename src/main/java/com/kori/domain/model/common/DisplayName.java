package com.kori.domain.model.common;

import java.util.Objects;

public final class DisplayName {

    public static final int MAX_LENGTH = 50;

    private final String value;

    private DisplayName(String value) {
        this.value = value;
    }

    public static DisplayName of(String raw) {
        Objects.requireNonNull(raw, "displayName");
        String normalized = raw.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("displayName must be <= " + MAX_LENGTH + " characters");
        }
        return new DisplayName(normalized);
    }

    public static DisplayName ofNullable(String raw) {
        if (raw == null) {
            return null;
        }
        return of(raw);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DisplayName other && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
