package com.kori.domain.model.admin;

import java.util.Objects;
import java.util.UUID;

public record AdminId(UUID value) {
    public AdminId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdminId other)) return false;
        return value.equals(other.value);
    }
}
