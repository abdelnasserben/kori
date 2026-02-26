package com.kori.query.model.me;

import java.math.BigDecimal;
import java.time.Instant;

public final class MeQueryModels {
    private MeQueryModels() {}

    public record ClientProfile(
            String code,
            String displayName,
            String phone,
            String status,
            Instant createdAt) {}

    public record MerchantProfile(
            String code,
            String displayName,
            String status,
            Instant createdAt) {}

    public record AgentProfile(
            String code,
            String displayName,
            String status,
            Instant createdAt) {}

    public record ActorBalance(
            String ownerRef,
            String currency,
            java.util.List<BalanceItem> balances) {}

    public record BalanceItem(
            String kind,
            BigDecimal amount) {}

    public record MeCardItem(
            String cardUid,
            String status,
            Instant createdAt) {}

    public record MeTransactionItem(
            String transactionRef,
            String type,
            String status,
            BigDecimal amount,
            String currency,
            Instant createdAt) {}

    public record ClientTransactionDetails(
            String transactionRef,
            String type,
            String status,
            BigDecimal amount,
            BigDecimal fee,
            BigDecimal totalDebited,
            String currency,
            String merchantCode,
            String originalTransactionRef,
            Instant createdAt) {}

    public record MerchantTransactionDetails(
            String transactionRef,
            String type,
            String status,
            BigDecimal amount,
            BigDecimal fee,
            BigDecimal totalDebited,
            String currency,
            String agentCode,
            String clientCode,
            String originalTransactionRef,
            Instant createdAt) {}

    public record AgentTransactionDetails(
            String transactionRef,
            String type,
            String status,
            BigDecimal amount,
            BigDecimal fee,
            BigDecimal totalDebited,
            String currency,
            String clientCode,
            String merchantCode,
            String terminalUid,
            String originalTransactionRef,
            Instant createdAt) {}

    public record MeTerminalItem(
            String terminalUid,
            String status,
            Instant createdAt,
            Instant lastSeen,
            String merchantCode) {}

    public record MeTransactionsFilter(
            String type,
            String status,
            Instant from,
            Instant to,
            BigDecimal min,
            BigDecimal max,
            Integer limit,
            String cursor,
            String sort) {}

    public record MeTerminalsFilter(
            String status,
            String terminalUid,
            Integer limit,
            String cursor,
            String sort) {}
}
