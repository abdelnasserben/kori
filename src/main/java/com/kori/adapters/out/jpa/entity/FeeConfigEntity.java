package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@Entity
@Table(name = "fee_config")
@Access(AccessType.FIELD)
public class FeeConfigEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "card_enrollment_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal cardEnrollmentPrice;

    @Column(name = "card_payment_fee_rate", nullable = false, precision = 10, scale = 6)
    private BigDecimal cardPaymentFeeRate;

    @Column(name = "card_payment_fee_min", nullable = false, precision = 19, scale = 2)
    private BigDecimal cardPaymentFeeMin;

    @Column(name = "card_payment_fee_max", nullable = false, precision = 19, scale = 2)
    private BigDecimal cardPaymentFeeMax;

    @Column(name = "merchant_withdraw_fee_rate", nullable = false, precision = 10, scale = 6)
    private BigDecimal merchantWithdrawFeeRate;

    @Column(name = "merchant_withdraw_fee_min", nullable = false, precision = 19, scale = 2)
    private BigDecimal merchantWithdrawFeeMin;

    @Column(name = "merchant_withdraw_fee_max", nullable = false, precision = 19, scale = 2)
    private BigDecimal merchantWithdrawFeeMax;

    protected FeeConfigEntity() { }

    public FeeConfigEntity(
            Integer id,
            BigDecimal cardEnrollmentPrice,
            BigDecimal cardPaymentFeeRate,
            BigDecimal cardPaymentFeeMin,
            BigDecimal cardPaymentFeeMax,
            BigDecimal merchantWithdrawFeeRate,
            BigDecimal merchantWithdrawFeeMin,
            BigDecimal merchantWithdrawFeeMax
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.cardEnrollmentPrice = Objects.requireNonNull(cardEnrollmentPrice, "cardEnrollmentPrice");
        this.cardPaymentFeeRate = Objects.requireNonNull(cardPaymentFeeRate, "cardPaymentFeeRate");
        this.cardPaymentFeeMin = Objects.requireNonNull(cardPaymentFeeMin, "cardPaymentFeeMin");
        this.cardPaymentFeeMax = Objects.requireNonNull(cardPaymentFeeMax, "cardPaymentFeeMax");
        this.merchantWithdrawFeeRate = Objects.requireNonNull(merchantWithdrawFeeRate, "merchantWithdrawFeeRate");
        this.merchantWithdrawFeeMin = Objects.requireNonNull(merchantWithdrawFeeMin, "merchantWithdrawFeeMin");
        this.merchantWithdrawFeeMax = Objects.requireNonNull(merchantWithdrawFeeMax, "merchantWithdrawFeeMax");
    }
}
