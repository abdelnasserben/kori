package com.kori.domain.model.terminal;

import java.util.Objects;

public final class TerminalId {
    private final String value;

    private TerminalId(String value) {
        this.value = Objects.requireNonNull(value);
    }

    public static TerminalId of(String value) {
        return new TerminalId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TerminalId other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
