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

    @Column(name = "client_transfer_max_per_transaction", nullable = false, precision = 19, scale = 2)
    private BigDecimal clientTransferMaxPerTransaction;

    @Column(name = "client_transfer_daily_max", nullable = false, precision = 19, scale = 2)
    private BigDecimal clientTransferDailyMax;

    protected PlatformConfigEntity() { }

    public PlatformConfigEntity(Integer id,
                                BigDecimal agentCashLimitGlobal,
                                BigDecimal clientTransferMaxPerTransaction,
                                BigDecimal clientTransferDailyMax) {
        this.id = Objects.requireNonNull(id, "id");
        this.agentCashLimitGlobal = Objects.requireNonNull(agentCashLimitGlobal, "agentCashLimitGlobal");
        this.clientTransferMaxPerTransaction = Objects.requireNonNull(clientTransferMaxPerTransaction, "clientTransferMaxPerTransaction");
        this.clientTransferDailyMax = Objects.requireNonNull(clientTransferDailyMax, "clientTransferDailyMax");
    }
}
