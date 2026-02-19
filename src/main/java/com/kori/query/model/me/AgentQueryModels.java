package com.kori.query.model.me;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public final class AgentQueryModels {
    private AgentQueryModels() {
    }

    public record AgentSummary(
            String code,
            String status,
            BigDecimal cashBalance,
            BigDecimal commissionBalance,
            Long txCount7d) {
    }

    public record AgentTransactionFilter(
            String type,
            String status,
            Instant from,
            Instant to,
            BigDecimal min,
            BigDecimal max,
            Integer limit,
            String cursor,
            String sort) {
    }

    public record AgentTransactionItem(
            String transactionRef,
            String type,
            String status,
            BigDecimal amount,
            String currency,
            Instant createdAt) {
    }

    public record AgentActivityFilter(
            String action,
            Instant from,
            Instant to,
            Integer limit,
            String cursor,
            String sort) {
    }

    public record AgentActivityItem(
            String eventRef,
            Instant occurredAt,
            String action,
            String resourceType,
            String resourceRef,
            Map<String, Object> metadata) {
    }

    public record AgentSearchFilter(
            String phone,
            String cardUid,
            String terminalUid,
            Integer limit) {
    }

    public record AgentSearchItem(
            String entityType,
            String entityRef,
            String display,
            String status,
            Map<String, String> links) {
    }
}
