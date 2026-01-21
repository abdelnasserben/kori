package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "commission_config")
@Access(AccessType.FIELD)
public class CommissionConfigEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "card_enrollment_agent_commission", nullable = false, precision = 19, scale = 2)
    private BigDecimal cardEnrollmentAgentCommission;

    @Column(name = "merchant_withdraw_commission_rate", nullable = false, precision = 10, scale = 6)
    private BigDecimal merchantWithdrawCommissionRate;

    @Column(name = "merchant_withdraw_commission_min", precision = 19, scale = 2)
    private BigDecimal merchantWithdrawCommissionMin;

    @Column(name = "merchant_withdraw_commission_max", precision = 19, scale = 2)
    private BigDecimal merchantWithdrawCommissionMax;

    protected CommissionConfigEntity() { }

}
