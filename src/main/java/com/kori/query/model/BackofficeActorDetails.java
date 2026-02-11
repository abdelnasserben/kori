package com.kori.query.model;

import java.time.Instant;

public record BackofficeActorDetails(
        String actorId,
        String display,
        String status,
        Instant createdAt,
        Instant lastActivityAt
) {
}

