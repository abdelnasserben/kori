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

    protected PlatformConfigEntity() { }

    public PlatformConfigEntity(Integer id, BigDecimal agentCashLimitGlobal) {
        this.id = Objects.requireNonNull(id, "id");
        this.agentCashLimitGlobal = Objects.requireNonNull(agentCashLimitGlobal, "agentCashLimitGlobal");
    }
}
