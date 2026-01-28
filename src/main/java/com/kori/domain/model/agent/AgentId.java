package com.kori.domain.model.agent;

import java.util.Objects;
import java.util.UUID;

public record AgentId(UUID value) {
    public AgentId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgentId other)) return false;
        return value.equals(other.value);
    }
}
