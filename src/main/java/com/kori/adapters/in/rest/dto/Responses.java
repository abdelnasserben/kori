package com.kori.adapters.in.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class Responses {
    private Responses() {
    }

    public record CreateAdminResponse(String adminId) {}

    public record CreateAgentResponse(String agentId, String agentCode) {}

    public record CreateMerchantResponse(String merchantId, String code) {}

    public record CreateTerminalResponse(String terminalId, String merchantCode) {}

    public record EnrollCardResponse(
            String transactionId,
            String clientPhoneNumber,
            String cardUid,
            BigDecimal cardPrice,
            BigDecimal agentCommission,
            boolean clientCreated,
            boolean clientAccountProfileCreated
    ) {}

    public record PayByCardResponse(
            String transactionId,
            String merchantCode,
            String cardUid,
            BigDecimal amount,
            BigDecimal fee,
            BigDecimal totalDebited
    ) {}

    public record MerchantWithdrawAtAgentResponse(
            String transactionId,
            String merchantCode,
            String agentCode,
            BigDecimal amount,
            BigDecimal fee,
            BigDecimal commission,
            BigDecimal totalDebitedMerchant
    ) {}

    public record CashInByAgentResponse(
            String transactionId,
            String agentId,
            String clientId,
            String clientPhoneNumber,
            BigDecimal amount
    ) {}

    public record AgentBankDepositReceiptResponse(
            String transactionId,
            String agentCode,
            BigDecimal amount
    ) {}

    public record AgentPayoutResponse(
            String transactionId,
            String payoutId,
            String agentCode,
            BigDecimal amount,
            String status
    ) {}

    public record ReversalResponse(String transactionId, String originalTransactionId) {}

    public record BalanceResponse(String accountType, String ownerRef, BigDecimal balance) {}

    /**
     * Standard cursor-based pagination metadata for list endpoints.
     */
    public record CursorPageResponse(String nextCursor) {}

    public record TransactionHistoryResponse(
            LedgerScope ledgerScope,
            List<TransactionHistoryItemResponse> items,
            Instant nextBeforeCreatedAt,
            String nextBeforeTransactionId
    ) {}

    public record LedgerScope(String accountType, String ownerRef) {}

    public record TransactionHistoryItemResponse(
            String transactionId,
            String transactionType,
            Instant createdAt,
            String clientId,
            String merchantId,
            String agentId,
            BigDecimal selfTotalDebits,
            BigDecimal selfTotalCredits,
            BigDecimal selfNet,
            BigDecimal amount,
            BigDecimal fee,
            BigDecimal totalDebited
    ) {}

    public record UpdateStatusResponse(String subjectId, String previousStatus, String newStatus) {}

    public record UpdateAccountProfileStatusResponse(
            String accountType,
            String ownerRef,
            String previousStatus,
            String newStatus
    ) {}

    public record UpdateFeeConfigResponse(
            BigDecimal cardEnrollmentPrice,
            BigDecimal cardPaymentFeeRate,
            BigDecimal cardPaymentFeeMin,
            BigDecimal cardPaymentFeeMax,
            BigDecimal merchantWithdrawFeeRate,
            BigDecimal merchantWithdrawFeeMin,
            BigDecimal merchantWithdrawFeeMax,
            boolean cardPaymentFeeRefundable,
            boolean merchantWithdrawFeeRefundable,
            boolean cardEnrollmentPriceRefundable
    ) {}

    public record UpdateCommissionConfigResponse(
            BigDecimal cardEnrollmentAgentCommission,
            BigDecimal merchantWithdrawCommissionRate,
            BigDecimal merchantWithdrawCommissionMin,
            BigDecimal merchantWithdrawCommissionMax
    ) {}
}
