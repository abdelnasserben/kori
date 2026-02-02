package com.kori.application.result;

import java.math.BigDecimal;

public record UpdateFeeConfigResult(
        BigDecimal cardEnrollmentPrice,
        BigDecimal cardPaymentFeeRate,
        BigDecimal cardPaymentFeeMin,
        BigDecimal cardPaymentFeeMax,
        BigDecimal merchantWithdrawFeeRate,
        BigDecimal merchantWithdrawFeeMin,
        BigDecimal merchantWithdrawFeeMax
) {
}
