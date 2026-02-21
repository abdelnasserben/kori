package com.kori.application.result;

import java.math.BigDecimal;

public record FeeConfigResult(
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
}
