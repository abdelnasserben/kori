package com.kori.integration.usecase;

import com.kori.application.command.AdminUpdateClientStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.AdminUpdateClientStatusUseCase;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.client.AdminClientStatusAction;
import com.kori.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminUpdateClientStatusForbiddenWhenClientNotFoundIT extends AbstractIntegrationTest {

    @Autowired AdminUpdateClientStatusUseCase adminUpdateClientStatusUseCase;

    @Test
    void adminUpdateClientStatus_isForbidden_whenClientDoesNotExist() {
        // Given
        String nonExistingClientId = UUID.randomUUID().toString();

        long auditBefore = auditEventJpaRepository.count();
        long idemBefore = idempotencyJpaRepository.count();

        // When / Then
        assertThrows(ForbiddenOperationException.class, () ->
                adminUpdateClientStatusUseCase.execute(new AdminUpdateClientStatusCommand(
                        idemKey("it-admin-client-status-not-found"),
                        new ActorContext(ActorType.ADMIN, "admin-actor-it", Map.of()),
                        nonExistingClientId,
                        AdminClientStatusAction.SUSPENDED,
                        "client not found"
                ))
        );

        // And: no side effects
        assertEquals(auditBefore, auditEventJpaRepository.count(), "No audit event should be written");
        assertEquals(idemBefore, idempotencyJpaRepository.count(), "No idempotency record should be written");
    }
}
