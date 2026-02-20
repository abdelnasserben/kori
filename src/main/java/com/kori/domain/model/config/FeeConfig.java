package com.kori.domain.model.config;

import java.math.BigDecimal;
import java.util.Objects;

public record FeeConfig(
        BigDecimal cardEnrollmentPrice,
        BigDecimal cardPaymentFeeRate,
        BigDecimal cardPaymentFeeMin,
        BigDecimal cardPaymentFeeMax,
        BigDecimal merchantWithdrawFeeRate,
        BigDecimal merchantWithdrawFeeMin,
        BigDecimal merchantWithdrawFeeMax,
        BigDecimal clientTransferFeeRate,
        BigDecimal clientTransferFeeMin,
        BigDecimal clientTransferFeeMax,
        boolean cardPaymentFeeRefundable,
        boolean merchantWithdrawFeeRefundable,
        boolean cardEnrollmentPriceRefundable
) {
    public FeeConfig {
        Objects.requireNonNull(cardEnrollmentPrice, "cardEnrollmentPrice");
        Objects.requireNonNull(cardPaymentFeeRate, "cardPaymentFeeRate");
        Objects.requireNonNull(cardPaymentFeeMin, "cardPaymentFeeMin");
        Objects.requireNonNull(cardPaymentFeeMax, "cardPaymentFeeMax");
        Objects.requireNonNull(merchantWithdrawFeeRate, "merchantWithdrawFeeRate");
        Objects.requireNonNull(merchantWithdrawFeeMin, "merchantWithdrawFeeMin");
        Objects.requireNonNull(merchantWithdrawFeeMax, "merchantWithdrawFeeMax");
        Objects.requireNonNull(clientTransferFeeRate, "clientTransferFeeRate");
        Objects.requireNonNull(clientTransferFeeMin, "clientTransferFeeMin");
        Objects.requireNonNull(clientTransferFeeMax, "clientTransferFeeMax");
    }

    public FeeConfig(
            BigDecimal cardEnrollmentPrice,
            BigDecimal cardPaymentFeeRate,
            BigDecimal cardPaymentFeeMin,
            BigDecimal cardPaymentFeeMax,
            BigDecimal merchantWithdrawFeeRate,
            BigDecimal merchantWithdrawFeeMin,
            BigDecimal merchantWithdrawFeeMax,
            BigDecimal clientTransferFeeRate,
            BigDecimal clientTransferFeeMin,
            BigDecimal clientTransferFeeMax
    ) {
        this(
                cardEnrollmentPrice,
                cardPaymentFeeRate,
                cardPaymentFeeMin,
                cardPaymentFeeMax,
                merchantWithdrawFeeRate,
                merchantWithdrawFeeMin,
                merchantWithdrawFeeMax,
                clientTransferFeeRate,
                clientTransferFeeMin,
                clientTransferFeeMax,
                false,
                false,
                false
        );
    }
}
