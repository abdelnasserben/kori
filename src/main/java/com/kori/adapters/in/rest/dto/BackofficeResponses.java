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

    public record TransactionLedgerLine(
            String accountType,
            String ownerRef,
            String entryType,
            BigDecimal amount,
            String currency
    ) {}

    public record TransactionPayout(
            String payoutId,
            String status,
            BigDecimal amount,
            Instant createdAt,
            Instant completedAt,
            Instant failedAt,
            String failureReason
    ) {}

    public record TransactionClientRefund(
            String refundId,
            String status,
            BigDecimal amount,
            Instant createdAt,
            Instant completedAt,
            Instant failedAt,
            String failureReason
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
            String clientPhone,
            String merchantId,
            String agentId,
            String terminalUid,
            String cardUid,
            String originalTransactionId,
            TransactionPayout payout,
            TransactionClientRefund clientRefund,
            List<TransactionLedgerLine> ledgerLines,
            Instant createdAt
    ) {}

    public record AuditEventItem(
            String eventId,
            Instant occurredAt,
            String actorType,
            String actorRef,
            String action,
            String resourceType,
            String resourceId,
            Map<String, Object> metadata
    ) {}

    public record ActorItem(
            String actorRef,
            String code,
            String status,
            Instant createdAt
    ) {}

    public record ActorDetails(
            String actorRef,
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
