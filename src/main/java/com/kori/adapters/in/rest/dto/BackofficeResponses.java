package com.kori.adapters.in.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class BackofficeResponses {
    private BackofficeResponses() {}

    public record CursorPage(String nextCursor, boolean hasMore) {}

    public record ListResponse<T>(List<T> items, CursorPage page) {}

    public record TransactionItem(
            String transactionId,
            String type,
            String status,
            BigDecimal amount,
            String currency,
            String merchantCode,
            String agentCode,
            String clientId,
            Instant createdAt
    ) {}

    public record TransactionDetails(
            String transactionId,
            String type,
            String status,
            BigDecimal amount,
            String currency,
            String merchantCode,
            String agentCode,
            String clientId,
            String originalTransactionId,
            Instant createdAt
    ) {}

    public record AuditEventItem(
            String eventId,
            Instant occurredAt,
            String actorType,
            String actorId,
            String action,
            String resourceType,
            String resourceId,
            Map<String, Object> metadata
    ) {}

    public record ActorItem(
            String actorId,
            String code,
            String status,
            Instant createdAt
    ) {}

    public record ActorDetails(
            String actorId,
            String display,
            String status,
            Instant createdAt,
            Instant lastActivityAt
    ) {}

    public record LookupItem(
            String entityType,
            String entityId,
            String display,
            String status,
            String detailUrl
    ) {}
}
