package com.kori.adapters.out.jpa.query;

import java.time.Instant;

public record BackofficeAuditEventQuery(
        String action,
        String actorType,
        String actorId,
        Instant from,
        Instant to,
        Integer limit,
        String cursor,
        String sort
) {
}
