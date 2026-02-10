package com.kori.adapters.out.jpa.query;

import java.math.BigDecimal;
import java.time.Instant;

public record BackofficeTransactionQuery(
        String query,
        String type,
        String status,
        String actorType,
        String actorId,
        Instant from,
        Instant to,
        BigDecimal min,
        BigDecimal max,
        Integer limit,
        String cursor,
        String sort
) {
}
