package com.kori.integration.usecase;

import com.kori.application.command.AdminUpdateClientStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.AdminUpdateClientStatusUseCase;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.client.AdminClientStatusAction;
import com.kori.integration.AbstractIntegrationTest;
import com.kori.integration.fixture.ClientSqlFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminUpdateClientStatusForbiddenWhenActorNotAdminIT extends AbstractIntegrationTest {

    @Autowired AdminUpdateClientStatusUseCase adminUpdateClientStatusUseCase;

    @Test
    void adminUpdateClientStatus_isForbidden_whenActorIsNotAdmin() {
        // Given: client exists
        ClientSqlFixture clientFixture = new ClientSqlFixture(jdbcTemplate);

        UUID clientId = uuid();
        String phone = randomPhone269();
        clientFixture.insertClient(clientId, phone, "ACTIVE");

        String statusBefore = jdbcTemplate.queryForObject(
                "select status from clients where id = ?",
                String.class,
                clientId
        );

        long auditBefore = auditEventJpaRepository.count();
        long idemBefore = idempotencyJpaRepository.count();

        // When / Then: AGENT tries to update client
        assertThrows(ForbiddenOperationException.class, () ->
                adminUpdateClientStatusUseCase.execute(new AdminUpdateClientStatusCommand(
                        idemKey("it-admin-client-status-forbidden"),
                        new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                        clientId.toString(),
                        AdminClientStatusAction.SUSPENDED,
                        "not allowed"
                ))
        );

        // Then: status unchanged
        String statusAfter = jdbcTemplate.queryForObject(
                "select status from clients where id = ?",
                String.class,
                clientId
        );
        assertEquals(statusBefore, statusAfter, "Client status must not change");

        // And: no side effects
        assertEquals(auditBefore, auditEventJpaRepository.count(), "No audit event should be written");
        assertEquals(idemBefore, idempotencyJpaRepository.count(), "No idempotency record should be written");
    }
}
