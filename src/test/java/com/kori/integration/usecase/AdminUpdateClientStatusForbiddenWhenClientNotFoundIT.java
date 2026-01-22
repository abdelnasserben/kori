package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.repo.AuditEventJpaRepository;
import com.kori.adapters.out.jpa.repo.IdempotencyJpaRepository;
import com.kori.application.command.AdminUpdateClientStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.AdminUpdateClientStatusUseCase;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.client.AdminClientStatusAction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class AdminUpdateClientStatusForbiddenWhenClientNotFoundIT {

    @Autowired AdminUpdateClientStatusUseCase adminUpdateClientStatusUseCase;

    @Autowired AuditEventJpaRepository auditEventJpaRepository;
    @Autowired IdempotencyJpaRepository idempotencyJpaRepository;

    @Test
    void adminUpdateClientStatus_isForbidden_whenClientDoesNotExist() {
        // Given
        String nonExistingClientId = UUID.randomUUID().toString();

        long auditBefore = auditEventJpaRepository.count();
        long idemBefore = idempotencyJpaRepository.count();

        // When / Then
        assertThrows(ForbiddenOperationException.class, () ->
                adminUpdateClientStatusUseCase.execute(new AdminUpdateClientStatusCommand(
                        "it-admin-client-status-not-found-" + UUID.randomUUID(),
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
