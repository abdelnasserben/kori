package com.kori.it;

import com.kori.application.port.out.FeeConfigPort;
import com.kori.application.port.out.PlatformConfigPort;
import com.kori.domain.model.config.FeeConfig;
import com.kori.domain.model.config.PlatformConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigPersistenceIT extends IntegrationTestBase {

    @Autowired
    FeeConfigPort feeConfigPort;

    @Autowired
    PlatformConfigPort platformConfigPort;

    @Test
    void feeConfig_persistsRefundableFlags() {
        FeeConfig updated = new FeeConfig(
                new BigDecimal("13.00"),
                new BigDecimal("0.040000"),
                new BigDecimal("2.00"),
                new BigDecimal("9.00"),
                new BigDecimal("0.050000"),
                new BigDecimal("2.50"),
                new BigDecimal("10.00"),
                true,
                true,
                true
        );

        feeConfigPort.upsert(updated);

        FeeConfig stored = feeConfigPort.get().orElseThrow();
        assertTrue(stored.cardPaymentFeeRefundable());
        assertTrue(stored.merchantWithdrawFeeRefundable());
        assertTrue(stored.cardEnrollmentPriceRefundable());
    }

    @Test
    void platformConfig_readsAndWritesAgentCashLimitGlobal() {
        platformConfigPort.upsert(new PlatformConfig(new BigDecimal("12345.67")));

        PlatformConfig cfg = platformConfigPort.get().orElseThrow();
        assertEquals(new BigDecimal("12345.67"), cfg.agentCashLimitGlobal());
    }
}
