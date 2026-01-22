package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.AccountEntity;
import com.kori.application.command.AdminUpdateAccountStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.AdminUpdateAccountStatusUseCase;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.account.AdminAccountStatusAction;
import com.kori.integration.AbstractIntegrationTest;
import com.kori.integration.fixture.AccountFixture;
import com.kori.integration.fixture.ClientSqlFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminUpdateAccountStatusForbiddenWhenActorNotAdminIT extends AbstractIntegrationTest {

    @Autowired AdminUpdateAccountStatusUseCase adminUpdateAccountStatusUseCase;

    @Test
    void adminUpdateAccountStatus_isForbidden_whenActorIsNotAdmin() {
        // Given: create client + account
        ClientSqlFixture clientFixture = new ClientSqlFixture(jdbcTemplate);
        AccountFixture accountFixture = new AccountFixture(accountJpaRepository);

        UUID clientId = clientFixture.insertActiveClient(randomPhone269());
        AccountEntity account = accountFixture.createActiveForClient(clientId);

        String statusBefore = accountJpaRepository.findById(account.getId())
                .orElseThrow(() -> new AssertionError("Account should exist"))
                .getStatus();

        long auditBefore = auditEventJpaRepository.count();
        long idemBefore = idempotencyJpaRepository.count();

        // When / Then: non-admin actor tries
        assertThrows(ForbiddenOperationException.class, () ->
                adminUpdateAccountStatusUseCase.execute(new AdminUpdateAccountStatusCommand(
                        idemKey("it-admin-account-status-forbidden"),
                        new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                        account.getId().toString(),
                        AdminAccountStatusAction.SUSPENDED,
                        "not allowed"
                ))
        );

        // Then: status unchanged
        AccountEntity after = accountJpaRepository.findById(account.getId())
                .orElseThrow(() -> new AssertionError("Account should exist"));
        assertEquals(statusBefore, after.getStatus(), "Account status must not change");

        // And: no side effects
        assertEquals(auditBefore, auditEventJpaRepository.count(), "No audit event should be written");
        assertEquals(idemBefore, idempotencyJpaRepository.count(), "No idempotency record should be written");
    }
}
