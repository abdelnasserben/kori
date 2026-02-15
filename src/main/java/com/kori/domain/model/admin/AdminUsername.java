package com.kori.domain.model.admin;

import java.util.Objects;
import java.util.regex.Pattern;

public final class AdminUsername {
    private static final Pattern FORMAT = Pattern.compile("^[A-Za-z0-9._@-]{3,64}$");
    private final String value;

    private AdminUsername(String value) {
        this.value = value;
    }

    public static AdminUsername of(String raw) {
        Objects.requireNonNull(raw, "adminUsername");
        String normalized = raw.trim();
        if (!FORMAT.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid adminUsername format.");
        }
        return new AdminUsername(normalized);
    }

    public String value() { return value; }

    @Override
    public String toString() { return value; }

    @Override
    public boolean equals(Object o) { return o instanceof AdminUsername other && value.equals(other.value); }

    @Override
    public int hashCode() { return value.hashCode(); }
}
