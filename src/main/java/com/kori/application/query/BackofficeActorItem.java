package com.kori.application.query;

import java.time.Instant;

public record BackofficeActorItem(
        String actorId,
        String code,
        String status,
        Instant createdAt
) {
}
