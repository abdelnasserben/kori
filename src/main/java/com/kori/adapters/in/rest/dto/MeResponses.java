package com.kori.adapters.in.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class MeResponses {
    private MeResponses() {}

    public record CursorPage(String nextCursor, boolean hasMore) {}

    public record ListResponse<T>(
            List<T> items,
            CursorPage page) {}

    public record ClientProfileResponse(
            String code,
            String displayName,
            String phone,
            String status,
            Instant createdAt) {}

    public record MerchantProfileResponse(
            String code,
            String displayName,
            String status,
            Instant createdAt) {}

    public record AgentProfileResponse(
            String code,
            String displayName,
            String status,
            Instant createdAt) {}

    public record BalanceItemResponse(String kind, BigDecimal amount) {}

    public record ActorBalanceResponse(String ownerRef, String currency, List<BalanceItemResponse> balances) {}

    public record CardItem(
            String cardUid,
            String status,
            Instant createdAt) {}

    public record TransactionDetailsResponse(
            String transactionRef,
            String type,
            String status,
            BigDecimal amount,
            BigDecimal fee,
            BigDecimal totalDebited,
            String currency,
            String clientCode,
            String merchantCode,
            String agentCode,
            String terminalUid,
            String originalTransactionRef,
            Instant createdAt) {}

    public record TransactionItem(
            String transactionRef,
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

    public record AlertItem(String code, String message) {}

    public record Kpis7dResponse(Long txCount, BigDecimal txVolume, Long failedCount) {}

    public record TerminalsSummaryResponse(Long total, Map<String, Long> byStatus, Long staleTerminals) {}

    public record ClientDashboardResponse(
            ClientProfileResponse profile,
            ActorBalanceResponse balance,
            List<CardItem> cards,
            List<TransactionItem> recentTransactions,
            List<AlertItem> alerts) {}

    public record MerchantDashboardResponse(
            MerchantProfileResponse profile,
            ActorBalanceResponse balance,
            Kpis7dResponse kpis7d,
            List<TransactionItem> recentTransactions,
            TerminalsSummaryResponse terminalsSummary) {}

    public record AgentDashboardResponse(
            AgentProfileResponse profile,
            ActorBalanceResponse balance,
            Kpis7dResponse kpis7d,
            List<TransactionItem> recentTransactions,
            List<AgentResponses.ActivityItem> recentActivities,
            List<AlertItem> alerts) {}
}
