package com.kori.domain.model.client;

import java.util.Objects;
import java.util.regex.Pattern;

public final class ClientCode {

    private static final Pattern FORMAT = Pattern.compile("^C-[0-9]{6}$");

    private final String value;

    private ClientCode(String value) {
        this.value = value;
    }

    public static ClientCode of(String raw) {
        Objects.requireNonNull(raw, "clientCode");
        String normalized = raw.trim().toUpperCase();
        if (!FORMAT.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid clientCode format. Expected C-XXXXXX (6 digits).");
        }
        return new ClientCode(normalized);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ClientCode other && value.equals(other.value);
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
