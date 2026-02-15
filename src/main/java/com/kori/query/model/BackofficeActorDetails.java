package com.kori.query.model;

import java.time.Instant;

public record BackofficeActorDetails(
        String actorRef,
        String display,
        String status,
        Instant createdAt,
        Instant lastActivityAt
) {
}

