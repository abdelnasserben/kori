package com.kori.adapters.out.jpa.query;

import java.time.Instant;
import java.util.Map;

public record BackofficeAuditEventItem(
        String eventId,
        Instant occurredAt,
        String actorType,
        String actorId,
        String action,
        String resourceType,
        String resourceId,
        Map<String, Object> metadata
) {
}
