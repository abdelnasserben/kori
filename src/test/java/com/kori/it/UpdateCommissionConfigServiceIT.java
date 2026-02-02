package com.kori.it;

import com.kori.application.command.UpdateCommissionConfigCommand;
import com.kori.application.port.in.UpdateCommissionConfigUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCommissionConfigServiceIT extends IntegrationTestBase {

    @Autowired
    UpdateCommissionConfigUseCase updateCommissionConfigUseCase;

    @Test
    void updateCommissionConfig_updatesRow_andWritesAudit() {
        updateCommissionConfigUseCase.execute(new UpdateCommissionConfigCommand(
                adminActor(),
                new BigDecimal("4.00"),
                new BigDecimal("0.600000"),
                new BigDecimal("0.80"),
                new BigDecimal("2.50"),
                "test"
        ));

        BigDecimal rate = jdbcTemplate.queryForObject(
                "select merchant_withdraw_commission_rate from commission_config where id = 1",
                BigDecimal.class
        );

        assertEquals(new BigDecimal("0.600000"), rate);

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().equals("ADMIN_UPDATE_COMMISSION_CONFIG"))
        );
    }
}
