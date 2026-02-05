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
    }

    public FeeConfig(
            BigDecimal cardEnrollmentPrice,
            BigDecimal cardPaymentFeeRate,
            BigDecimal cardPaymentFeeMin,
            BigDecimal cardPaymentFeeMax,
            BigDecimal merchantWithdrawFeeRate,
            BigDecimal merchantWithdrawFeeMin,
            BigDecimal merchantWithdrawFeeMax
    ) {
        this(
                cardEnrollmentPrice,
                cardPaymentFeeRate,
                cardPaymentFeeMin,
                cardPaymentFeeMax,
                merchantWithdrawFeeRate,
                merchantWithdrawFeeMin,
                merchantWithdrawFeeMax,
                false,
                false,
                false
        );
    }
}
