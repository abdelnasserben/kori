package com.kori.application.query;

import java.time.Instant;

public record BackofficeAuditEventQuery(
        String action,
        String actorType,
        String actorId,
        String resourceType,
        String resourceId,
        Instant from,
        Instant to,
        Integer limit,
        String cursor,
        String sort
) {
}
