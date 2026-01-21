package com.kori.application.port.out;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public record AuditEvent(String action, String actorType, String actorId, Instant occurredAt,
                         Map<String, String> metadata) {
    public AuditEvent(String action,
                      String actorType,
                      String actorId,
                      Instant occurredAt,
                      Map<String, String> metadata) {
        this.action = Objects.requireNonNull(action);
        this.actorType = Objects.requireNonNull(actorType);
        this.actorId = Objects.requireNonNull(actorId);
        this.occurredAt = Objects.requireNonNull(occurredAt);
        this.metadata = metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
    }
}
