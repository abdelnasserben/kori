package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.AccountEntity;
import com.kori.adapters.out.jpa.entity.IdempotencyRecordEntity;
import com.kori.application.command.AdminUpdateAccountStatusCommand;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AdminUpdateAccountStatusIT extends AbstractIntegrationTest {

    @Autowired AdminUpdateAccountStatusUseCase adminUpdateAccountStatusUseCase;

    @Test
    void happyPath_adminUpdatesAccountStatus_updatesDb_writesAudit_andIdempotency() {
        // Given: create CLIENT + ACCOUNT
        ClientSqlFixture clientFixture = new ClientSqlFixture(jdbcTemplate);
        AccountFixture accountFixture = new AccountFixture(accountJpaRepository);

        UUID clientId = clientFixture.insertActiveClient(randomPhone269());
        AccountEntity account = accountFixture.createActiveForClient(clientId);

        long auditBefore = auditEventJpaRepository.count();
        String idemKey = idemKey("it-admin-account-status");

        // When
        adminUpdateAccountStatusUseCase.execute(new AdminUpdateAccountStatusCommand(
                idemKey,
                new ActorContext(ActorType.ADMIN, "admin-actor-it", Map.of()),
                account.getId().toString(),
                AdminAccountStatusAction.SUSPENDED,
                "risk review"
        ));

        // Then
        AccountEntity after = accountJpaRepository.findById(account.getId())
                .orElseThrow(() -> new AssertionError("Account should still exist"));
        assertNotEquals("ACTIVE", after.getStatus(), "Account status should be updated");

        assertEquals(auditBefore + 1, auditEventJpaRepository.count(), "One audit event should be written");

        Optional<IdempotencyRecordEntity> idemOpt = idempotencyJpaRepository.findById(idemKey);
        assertTrue(idemOpt.isPresent(), "Idempotency record should exist");
        assertNotNull(idemOpt.get().getResultJson());
        assertFalse(idemOpt.get().getResultJson().isBlank());
    }
}
