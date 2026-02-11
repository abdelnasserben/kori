package com.kori.application.query;

import java.math.BigDecimal;
import java.time.Instant;

public record BackofficeTransactionQuery(
        String query,
        String type,
        String status,
        String actorType,
        String actorId,
        String terminalUid,
        String cardUid,
        String merchantId,
        String agentId,
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
