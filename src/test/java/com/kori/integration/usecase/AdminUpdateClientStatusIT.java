package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.IdempotencyRecordEntity;
import com.kori.application.command.AdminUpdateClientStatusCommand;
import com.kori.application.port.in.AdminUpdateClientStatusUseCase;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.client.AdminClientStatusAction;
import com.kori.integration.AbstractIntegrationTest;
import com.kori.integration.fixture.ClientSqlFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AdminUpdateClientStatusIT extends AbstractIntegrationTest {

    @Autowired AdminUpdateClientStatusUseCase adminUpdateClientStatusUseCase;

    @Test
    void happyPath_adminUpdatesClientStatus_updatesDb_writesAudit_andIdempotency() {
        // Given: create client in DB
        ClientSqlFixture clientFixture = new ClientSqlFixture(jdbcTemplate);

        UUID clientId = uuid();
        String phone = randomPhone269();

        clientFixture.insertClient(clientId, phone, "ACTIVE");

        String statusBefore = jdbcTemplate.queryForObject(
                "select status from clients where id = ?",
                String.class,
                clientId
        );
        assertNotNull(statusBefore);

        long auditBefore = auditEventJpaRepository.count();
        String idemKey = idemKey("it-admin-client-status");

        // When: ADMIN updates client status
        adminUpdateClientStatusUseCase.execute(new AdminUpdateClientStatusCommand(
                idemKey,
                new ActorContext(ActorType.ADMIN, "admin-actor-it", Map.of()),
                clientId.toString(),
                AdminClientStatusAction.SUSPENDED,
                "risk review"
        ));

        // Then: status updated in DB
        String statusAfter = jdbcTemplate.queryForObject(
                "select status from clients where id = ?",
                String.class,
                clientId
        );
        assertNotNull(statusAfter);
        assertNotEquals(statusBefore, statusAfter, "Client status should change");

        // Optionnel si tu connais la valeur exacte :
        assertEquals("SUSPENDED", statusAfter);

        // Then: audit written
        assertEquals(auditBefore + 1, auditEventJpaRepository.count(), "One audit event should be written");

        // Then: idempotency written
        Optional<IdempotencyRecordEntity> idemOpt = idempotencyJpaRepository.findById(idemKey);
        assertTrue(idemOpt.isPresent(), "Idempotency record should exist");
        assertNotNull(idemOpt.get().getResultJson());
        assertFalse(idemOpt.get().getResultJson().isBlank());
    }
}
