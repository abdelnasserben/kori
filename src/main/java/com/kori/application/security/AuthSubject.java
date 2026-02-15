package com.kori.application.security;

import java.util.Objects;

public record AuthSubject(String value) {

    public AuthSubject {
        Objects.requireNonNull(value, "auth sub is required");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("auth sub is required");
        }
    }

    public static AuthSubject of(String raw) {
        return new AuthSubject(raw);
    }
}
