package com.kori.query.model;

import java.time.Instant;

public record BackofficeActorItem(
        String actorRef,
        String displayName,
        String status,
        Instant createdAt
) {
}
