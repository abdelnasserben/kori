package com.kori.application.query;

import java.time.Instant;

public record BackofficeActorDetails(
        String actorId,
        String display,
        String status,
        Instant createdAt,
        Instant lastActivityAt
) {
}

