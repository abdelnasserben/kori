package com.kori.query.model;

import java.time.Instant;

public record BackofficeActorItem(
        String actorRef,
        String code,
        String status,
        Instant createdAt
) {
}
