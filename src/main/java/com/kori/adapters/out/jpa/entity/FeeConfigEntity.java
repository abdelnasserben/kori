package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

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

}
