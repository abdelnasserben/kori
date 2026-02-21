package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@Entity
@Table(name = "platform_config")
@Access(AccessType.FIELD)
public class PlatformConfigEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "agent_cash_limit_global", nullable = false, precision = 19, scale = 2)
    private BigDecimal agentCashLimitGlobal;

    @Column(name = "client_transfer_min_per_transaction", nullable = false, precision = 19, scale = 2)
    private BigDecimal clientTransferMinPerTransaction;

    @Column(name = "client_transfer_max_per_transaction", nullable = false, precision = 19, scale = 2)
    private BigDecimal clientTransferMaxPerTransaction;

    @Column(name = "client_transfer_daily_max", nullable = false, precision = 19, scale = 2)
    private BigDecimal clientTransferDailyMax;

    @Column(name = "merchant_transfer_min_per_transaction", nullable = false, precision = 19, scale = 2)
    private BigDecimal merchantTransferMinPerTransaction;

    @Column(name = "merchant_transfer_max_per_transaction", nullable = false, precision = 19, scale = 2)
    private BigDecimal merchantTransferMaxPerTransaction;

    @Column(name = "merchant_transfer_daily_max", nullable = false, precision = 19, scale = 2)
    private BigDecimal merchantTransferDailyMax;

    @Column(name = "merchant_withdraw_min_per_transaction", nullable = false, precision = 19, scale = 2)
    private BigDecimal merchantWithdrawMinPerTransaction;

    protected PlatformConfigEntity() { }

    public PlatformConfigEntity(Integer id,
                                BigDecimal agentCashLimitGlobal,
                                BigDecimal clientTransferMinPerTransaction,
                                BigDecimal clientTransferMaxPerTransaction,
                                BigDecimal clientTransferDailyMax,
                                BigDecimal merchantTransferMinPerTransaction,
                                BigDecimal merchantTransferMaxPerTransaction,
                                BigDecimal merchantTransferDailyMax,
                                BigDecimal merchantWithdrawMinPerTransaction) {
        this.id = Objects.requireNonNull(id, "id");
        this.agentCashLimitGlobal = Objects.requireNonNull(agentCashLimitGlobal, "agentCashLimitGlobal");
        this.clientTransferMinPerTransaction = Objects.requireNonNull(clientTransferMinPerTransaction, "clientTransferMinPerTransaction");
        this.clientTransferMaxPerTransaction = Objects.requireNonNull(clientTransferMaxPerTransaction, "clientTransferMaxPerTransaction");
        this.clientTransferDailyMax = Objects.requireNonNull(clientTransferDailyMax, "clientTransferDailyMax");
        this.merchantTransferMinPerTransaction = Objects.requireNonNull(merchantTransferMinPerTransaction, "merchantTransferMinPerTransaction");
        this.merchantTransferMaxPerTransaction = Objects.requireNonNull(merchantTransferMaxPerTransaction, "merchantTransferMaxPerTransaction");
        this.merchantTransferDailyMax = Objects.requireNonNull(merchantTransferDailyMax, "merchantTransferDailyMax");
        this.merchantWithdrawMinPerTransaction = Objects.requireNonNull(merchantWithdrawMinPerTransaction, "merchantWithdrawMinPerTransaction");
    }
}
