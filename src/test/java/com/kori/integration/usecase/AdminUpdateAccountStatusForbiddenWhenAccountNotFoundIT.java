package com.kori.integration.usecase;

import com.kori.application.command.AdminUpdateAccountStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.AdminUpdateAccountStatusUseCase;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.account.AdminAccountStatusAction;
import com.kori.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminUpdateAccountStatusForbiddenWhenAccountNotFoundIT extends AbstractIntegrationTest {

    @Autowired AdminUpdateAccountStatusUseCase adminUpdateAccountStatusUseCase;

    @Test
    void adminUpdateAccountStatus_isForbidden_whenAccountDoesNotExist() {
        // Given
        String nonExistingAccountId = UUID.randomUUID().toString();

        long auditBefore = auditEventJpaRepository.count();
        long idemBefore = idempotencyJpaRepository.count();

        // When / Then
        assertThrows(ForbiddenOperationException.class, () ->
                adminUpdateAccountStatusUseCase.execute(new AdminUpdateAccountStatusCommand(
                        idemKey("it-admin-account-status-not-found"),
                        new ActorContext(ActorType.ADMIN, "admin-actor-it", Map.of()),
                        nonExistingAccountId,
                        AdminAccountStatusAction.SUSPENDED,
                        "account not found"
                ))
        );

        // And: no side effects
        assertEquals(auditBefore, auditEventJpaRepository.count(), "No audit event should be written");
        assertEquals(idemBefore, idempotencyJpaRepository.count(), "No idempotency record should be written");
    }
}
