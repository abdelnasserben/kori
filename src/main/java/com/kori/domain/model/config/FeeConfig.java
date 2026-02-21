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
        BigDecimal merchantTransferFeeRate,
        BigDecimal merchantTransferFeeMin,
        BigDecimal merchantTransferFeeMax,
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
        Objects.requireNonNull(merchantTransferFeeRate, "merchantTransferFeeRate");
        Objects.requireNonNull(merchantTransferFeeMin, "merchantTransferFeeMin");
        Objects.requireNonNull(merchantTransferFeeMax, "merchantTransferFeeMax");
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
            BigDecimal clientTransferFeeMax,
            BigDecimal merchantTransferFeeRate,
            BigDecimal merchantTransferFeeMin,
            BigDecimal merchantTransferFeeMax
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
                merchantTransferFeeRate,
                merchantTransferFeeMin,
                merchantTransferFeeMax,
                false,
                false,
                false
        );
    }
}
