package com.kori.query.model;

import java.time.Instant;
import java.util.Map;

public record BackofficeAuditEventItem(
        String eventRef,
        Instant occurredAt,
        String actorType,
        String actorRef,
        String action,
        String resourceType,
        String resourceRef,
        Map<String, Object> metadata
) {
}
