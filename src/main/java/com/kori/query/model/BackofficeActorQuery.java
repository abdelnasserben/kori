package com.kori.query.model;

import java.time.Instant;

public record BackofficeActorQuery(
        String query,
        String status,
        Instant createdFrom,
        Instant createdTo,
        Integer limit,
        String cursor,
        String sort
) {
}
