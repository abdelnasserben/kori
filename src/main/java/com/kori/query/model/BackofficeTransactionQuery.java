package com.kori.query.model;

import java.math.BigDecimal;
import java.time.Instant;

public record BackofficeTransactionQuery(
        String query,
        String type,
        String status,
        String actorType,
        String actorRef,
        String terminalUid,
        String cardUid,
        String merchantCode,
        String agentCode,
        String clientPhone,
        Instant from,
        Instant to,
        BigDecimal min,
        BigDecimal max,
        Integer limit,
        String cursor,
        String sort
) {
}
