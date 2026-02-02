package com.kori.it;

import com.kori.application.command.UpdateFeeConfigCommand;
import com.kori.application.port.in.UpdateFeeConfigUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateFeeConfigServiceIT extends IntegrationTestBase {

    @Autowired
    UpdateFeeConfigUseCase updateFeeConfigUseCase;

    @Test
    void updateFeeConfig_updatesRow_andWritesAudit() {
        updateFeeConfigUseCase.execute(new UpdateFeeConfigCommand(
                adminActor(),
                new BigDecimal("12.00"),
                new BigDecimal("0.025000"),
                new BigDecimal("1.50"),
                new BigDecimal("6.50"),
                new BigDecimal("0.035000"),
                new BigDecimal("2.00"),
                new BigDecimal("7.00"),
                "test"
        ));

        BigDecimal rate = jdbcTemplate.queryForObject(
                "select card_payment_fee_rate from fee_config where id = 1",
                BigDecimal.class
        );

        assertEquals(new BigDecimal("0.025000"), rate);

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().equals("ADMIN_UPDATE_FEE_CONFIG"))
        );
    }
}
