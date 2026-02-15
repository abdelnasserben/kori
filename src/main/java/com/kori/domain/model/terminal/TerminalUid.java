package com.kori.domain.model.terminal;

import java.util.Objects;
import java.util.regex.Pattern;

public final class TerminalUid {
    private static final Pattern FORMAT = Pattern.compile("^[A-Z0-9][A-Z0-9-]{4,31}$");
    private final String value;

    private TerminalUid(String value) {
        this.value = value;
    }

    public static TerminalUid of(String raw) {
        Objects.requireNonNull(raw, "terminalUid");
        String normalized = raw.trim().toUpperCase();
        if (!FORMAT.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid terminalUid format.");
        }
        return new TerminalUid(normalized);
    }

    public String value() { return value; }

    @Override
    public String toString() { return value; }

    @Override
    public boolean equals(Object o) { return o instanceof TerminalUid other && value.equals(other.value); }

    @Override
    public int hashCode() { return value.hashCode(); }
}
