package com.kori.domain.model.client;

import java.util.Objects;
import java.util.regex.Pattern;

public final class PhoneNumber {
    private static final Pattern E164 = Pattern.compile("^\\+[1-9][0-9]{7,14}$");
    private final String value;

    private PhoneNumber(String value) {
        this.value = value;
    }

    public static PhoneNumber of(String raw) {
        Objects.requireNonNull(raw, "phoneNumber");
        String normalized = raw.trim();
        if (!E164.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid phoneNumber format. Expected E.164 (+XXXXXXXX).");
        }
        return new PhoneNumber(normalized);
    }

    public String value() { return value; }

    @Override
    public String toString() { return value; }

    @Override
    public boolean equals(Object o) { return o instanceof PhoneNumber other && value.equals(other.value); }

    @Override
    public int hashCode() { return value.hashCode(); }
}
