package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.math.BigDecimal;
import java.util.Objects;

public record UpdateFeeConfigCommand(
        ActorContext actorContext,
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
        boolean cardEnrollmentPriceRefundable,
        String reason
) {
    public UpdateFeeConfigCommand(
            ActorContext actorContext,
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
            boolean cardEnrollmentPriceRefundable,
            String reason) {
        this.actorContext = Objects.requireNonNull(actorContext, "actorContext");
        this.cardEnrollmentPrice = Objects.requireNonNull(cardEnrollmentPrice, "cardEnrollmentPrice");
        this.cardPaymentFeeRate = Objects.requireNonNull(cardPaymentFeeRate, "cardPaymentFeeRate");
        this.cardPaymentFeeMin = Objects.requireNonNull(cardPaymentFeeMin, "cardPaymentFeeMin");
        this.cardPaymentFeeMax = Objects.requireNonNull(cardPaymentFeeMax, "cardPaymentFeeMax");
        this.merchantWithdrawFeeRate = Objects.requireNonNull(merchantWithdrawFeeRate, "merchantWithdrawFeeRate");
        this.merchantWithdrawFeeMin = Objects.requireNonNull(merchantWithdrawFeeMin, "merchantWithdrawFeeMin");
        this.merchantWithdrawFeeMax = Objects.requireNonNull(merchantWithdrawFeeMax, "merchantWithdrawFeeMax");
        this.clientTransferFeeRate = Objects.requireNonNull(clientTransferFeeRate, "clientTransferFeeRate");
        this.clientTransferFeeMin = Objects.requireNonNull(clientTransferFeeMin, "clientTransferFeeMin");
        this.clientTransferFeeMax = Objects.requireNonNull(clientTransferFeeMax, "clientTransferFeeMax");
        this.cardPaymentFeeRefundable = cardPaymentFeeRefundable;
        this.merchantWithdrawFeeRefundable = merchantWithdrawFeeRefundable;
        this.cardEnrollmentPriceRefundable = cardEnrollmentPriceRefundable;
        this.reason = (reason == null || reason.isBlank()) ? "N/A" : reason;
    }
}
