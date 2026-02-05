package com.kori.adapters.in.rest.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * REST Request DTOs (adapter/in).
 * - Validation here is "API contract" validation (format/required/bounds).
 */
public final class Requests {

    /** PIN must be exactly 4 digits (matches domain rule). */
    public static final String PIN_REGEX_4_DIGITS = "\\d{4}";

    /**
     * Comoros phone numbers only.
     * Comoros country calling code: +269
     * National significant number length is typically 7 digits.
     */
    public static final String COMOROS_PHONE_REGEX = "^(\\+269)?\\d{7}$";

    private Requests() {
    }

    public record CreateTerminalRequest(
            @NotBlank @Size(max = 16) String merchantCode
    ) {}

    public record EnrollCardRequest(
            @Pattern(regexp = COMOROS_PHONE_REGEX, message = "Phone number must be (+269 + 7 digits) or 7 digits local")
            String phoneNumber,
            @NotBlank @Size(max = 64)
            String cardUid,
            @Pattern(regexp = PIN_REGEX_4_DIGITS, message = "PIN must be exactly 4 digits")
            String pin,
            @NotBlank @Size(max = 16)
            String agentCode
    ) {}

    public record PayByCardRequest(
            @NotBlank String terminalUid,
            @NotBlank @Size(max = 64) String cardUid,
            @Pattern(regexp = PIN_REGEX_4_DIGITS, message = "PIN must be exactly 4 digits")
            String pin,
            @NotNull @Positive BigDecimal amount
    ) {}

    public record MerchantWithdrawAtAgentRequest(
            @NotBlank @Size(max = 16) String merchantCode,
            @NotBlank @Size(max = 16) String agentCode,
            @NotNull @Positive BigDecimal amount
    ) {}

    public record CashInByAgentRequest(
            @NotBlank
            @Pattern(regexp = COMOROS_PHONE_REGEX, message = "Phone number must be (+269 + 7 digits) or 7 digits local")
            String phoneNumber,
            @NotNull @Positive BigDecimal amount
    ) {}

    public record RequestAgentPayoutRequest(
            @NotBlank @Size(max = 16) String agentCode
    ) {}

    public record ReversalRequest(@NotBlank String originalTransactionId) {}

    public record UpdateStatusRequest(
            @NotBlank String targetStatus,
            @Size(max = 255) String reason
    ) {}

    public record AgentCardStatusRequest(
            @NotBlank @Size(max = 16) String agentCode,
            @NotBlank String targetStatus,
            @Size(max = 255) String reason
    ) {}

    public record UpdateAccountProfileStatusRequest(
            @NotBlank String accountType,
            @NotBlank String ownerRef,
            @NotBlank String targetStatus,
            @Size(max = 255) String reason
    ) {}

    public record UpdateFeeConfigRequest(
            @NotNull @PositiveOrZero BigDecimal cardEnrollmentPrice,
            @NotNull @PositiveOrZero BigDecimal cardPaymentFeeRate,
            @NotNull @PositiveOrZero BigDecimal cardPaymentFeeMin,
            @NotNull @PositiveOrZero BigDecimal cardPaymentFeeMax,
            @NotNull @PositiveOrZero BigDecimal merchantWithdrawFeeRate,
            @NotNull @PositiveOrZero BigDecimal merchantWithdrawFeeMin,
            @NotNull @PositiveOrZero BigDecimal merchantWithdrawFeeMax,
            Boolean cardPaymentFeeRefundable,
            Boolean merchantWithdrawFeeRefundable,
            Boolean cardEnrollmentPriceRefundable,
            @Size(max = 255) String reason
    ) {}

    public record UpdateCommissionConfigRequest(
            @NotNull @PositiveOrZero BigDecimal cardEnrollmentAgentCommission,
            @NotNull @PositiveOrZero BigDecimal merchantWithdrawCommissionRate,
            @PositiveOrZero BigDecimal merchantWithdrawCommissionMin,
            @PositiveOrZero BigDecimal merchantWithdrawCommissionMax,
            @Size(max = 255) String reason
    ) {}

    public record FailPayoutRequest(@NotBlank @Size(max = 255) String reason) {}

    /**
     * Standard cursor-based pagination for list endpoints.
     * - cursor is opaque and must be returned as-is by clients.
     * - limit <= 0 means "use default".
     */
    public record CursorPageRequest(
            String cursor,
            @PositiveOrZero Integer limit
    ) {}

    public record SearchLedgerRequest(
            String accountType,
            String ownerRef,
            String transactionType,
            Instant from,
            Instant to,
            Instant beforeCreatedAt,
            String beforeTransactionId,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String view,
            @PositiveOrZero Integer limit
    ) {}
}
