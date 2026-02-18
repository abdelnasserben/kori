package com.kori.query.model;

import java.time.Instant;

public record BackofficeAuditEventQuery(
        String action,
        String actorType,
        String actorRef,
        String resourceType,
        String resourceRef,
        Instant from,
        Instant to,
        Integer limit,
        String cursor,
        String sort
) {
}
