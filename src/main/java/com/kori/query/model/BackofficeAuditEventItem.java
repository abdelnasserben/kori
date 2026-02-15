package com.kori.query.model;

import java.time.Instant;
import java.util.Map;

public record BackofficeAuditEventItem(
        String eventId,
        Instant occurredAt,
        String actorType,
        String actorRef,
        String action,
        String resourceType,
        String resourceId,
        Map<String, Object> metadata
) {
}
