package com.kori.application.query.model;

import java.math.BigDecimal;
import java.time.Instant;

public final class MeQueryModels {
    private MeQueryModels() {}

    public record MeProfile(
            String actorId,
            String code,
            String status,
            Instant createdAt) {}

    public record MeBalance(
            String accountType,
            String ownerRef,
            BigDecimal balance,
            String currency) {}

    public record MeCardItem(
            String cardUid,
            String status,
            Instant createdAt) {}

    public record MeTransactionItem(
            String transactionId,
            String type,
            String status,
            BigDecimal amount,
            String currency,
            Instant createdAt) {}

    public record ClientTransactionDetails(
            String transactionId,
            String type,
            String status,
            BigDecimal amount,
            String currency,
            String merchantCode,
            String originalTransactionId,
            Instant createdAt) {}

    public record MerchantTransactionDetails(
            String transactionId,
            String type,
            String status,
            BigDecimal amount,
            String currency,
            String agentCode,
            String clientId,
            String originalTransactionId,
            Instant createdAt) {}

    public record MeTerminalItem(
            String terminalUid,
            String status,
            Instant createdAt,
            Instant lastSeen,
            String merchantId) {}

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
            String query,
            Integer limit,
            String cursor,
            String sort) {}
}
