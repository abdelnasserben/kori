package com.kori.adapters.in.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AgentResponses {
    private AgentResponses() {
    }

    public record SummaryResponse(
            String agentId,
            String code,
            String status,
            BigDecimal cashBalance,
            BigDecimal commissionBalance,
            Long txCount7d) {
    }

    public record TransactionItem(
            String transactionId,
            String type,
            String status,
            BigDecimal amount,
            String currency,
            Instant createdAt) {
    }

    public record ActivityItem(
            String eventId,
            Instant occurredAt,
            String action,
            String resourceType,
            String resourceId,
            Map<String, Object> metadata) {
    }

    public record CursorPage(String nextCursor, boolean hasMore) {
    }

    public record ListResponse<T>(List<T> items, CursorPage page) {
    }

    public record SearchResponse(List<SearchItem> items) {
    }

    public record SearchItem(
            String entityType,
            String entityId,
            String display,
            String status,
            Map<String, String> links) {
    }
}
