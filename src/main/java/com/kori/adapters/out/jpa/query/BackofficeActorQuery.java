package com.kori.adapters.out.jpa.query;

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
