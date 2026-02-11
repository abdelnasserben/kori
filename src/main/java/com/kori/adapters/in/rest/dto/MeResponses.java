package com.kori.adapters.in.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class MeResponses {
    private MeResponses() {}

    public record CursorPage(String nextCursor, boolean hasMore) {}

    public record ListResponse<T>(
            List<T> items,
            CursorPage page) {}

    public record ProfileResponse(
            String actorId,
            String code,
            String status,
            Instant createdAt) {}

    public record BalanceResponse(
            String accountType,
            String ownerRef,
            BigDecimal balance,
            String currency) {}

    public record CardItem(
            String cardUid,
            String status,
            Instant createdAt) {}

    public record ClientTransactionDetailsResponse(
            String transactionId,
            String type,
            String status,
            BigDecimal amount,
            BigDecimal fee,
            BigDecimal totalDebited,
            String currency,
            String merchantCode,
            String originalTransactionId,
            Instant createdAt) {}

    public record MerchantTransactionDetailsResponse(
            String transactionId,
            String type,
            String status,
            BigDecimal amount,
            BigDecimal fee,
            BigDecimal totalDebited,
            String currency,
            String agentCode,
            String clientId,
            String originalTransactionId,
            Instant createdAt) {}

    public record TransactionItem(
            String transactionId,
            String type,
            String status,
            BigDecimal amount,
            String currency,
            Instant createdAt) {}

    public record TerminalItem(
            String terminalUid,
            String status,
            Instant createdAt,
            Instant lastSeen,
            String merchantCode) {}
}
