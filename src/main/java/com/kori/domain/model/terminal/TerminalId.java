package com.kori.domain.model.terminal;

import java.util.Objects;
import java.util.UUID;

public record TerminalId(UUID value) {
    public TerminalId(UUID value) {
        this.value = Objects.requireNonNull(value);
    }

    public static TerminalId newId() {
        return new TerminalId(UUID.randomUUID());
    }

    public static TerminalId of(String value) {
        return new TerminalId(UUID.fromString(value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TerminalId other)) return false;
        return value.equals(other.value);
    }

}
