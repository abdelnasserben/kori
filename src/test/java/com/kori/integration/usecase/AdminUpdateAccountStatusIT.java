package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.AccountEntity;
import com.kori.adapters.out.jpa.entity.ClientEntity;
import com.kori.adapters.out.jpa.entity.IdempotencyRecordEntity;
import com.kori.adapters.out.jpa.repo.AccountJpaRepository;
import com.kori.adapters.out.jpa.repo.AuditEventJpaRepository;
import com.kori.adapters.out.jpa.repo.ClientJpaRepository;
import com.kori.adapters.out.jpa.repo.IdempotencyJpaRepository;
import com.kori.application.command.AdminUpdateAccountStatusCommand;
import com.kori.application.port.in.AdminUpdateAccountStatusUseCase;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.account.AdminAccountStatusAction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AdminUpdateAccountStatusIT {

    @Autowired AdminUpdateAccountStatusUseCase adminUpdateAccountStatusUseCase;

    @Autowired AccountJpaRepository accountJpaRepository;
    @Autowired AuditEventJpaRepository auditEventJpaRepository;
    @Autowired IdempotencyJpaRepository idempotencyJpaRepository;
    @Autowired ClientJpaRepository clientJpaRepository;

    @Test
    void happyPath_adminUpdatesAccountStatus_updatesDb_writesAudit_andIdempotency() {
        // Given: create a CLIENT explicitly
        UUID clientId = UUID.randomUUID();
        ClientEntity client = new ClientEntity(clientId, "+269..." , "ACTIVE");
        clientJpaRepository.saveAndFlush(client);

        // Given: create an ACCOUNT linked to that client
        UUID accountId = UUID.randomUUID();
        AccountEntity account = new AccountEntity(accountId, clientId, "ACTIVE");
        accountJpaRepository.saveAndFlush(account);

        long auditBefore = auditEventJpaRepository.count();
        String idemKey = "it-admin-account-status-" + UUID.randomUUID();

        // When
        adminUpdateAccountStatusUseCase.execute(new AdminUpdateAccountStatusCommand(
                idemKey,
                new ActorContext(ActorType.ADMIN, "admin-actor-it", Map.of()),
                accountId.toString(),
                AdminAccountStatusAction.SUSPENDED,
                "risk review"
        ));

        // Then
        AccountEntity after = accountJpaRepository.findById(accountId)
                .orElseThrow(() -> new AssertionError("Account should still exist"));
        assertNotEquals("ACTIVE", after.getStatus(), "Account status should be updated");

        assertEquals(auditBefore + 1, auditEventJpaRepository.count(), "One audit event should be written");

        Optional<IdempotencyRecordEntity> idemOpt = idempotencyJpaRepository.findById(idemKey);
        assertTrue(idemOpt.isPresent(), "Idempotency record should exist");
        assertNotNull(idemOpt.get().getResultJson());
        assertFalse(idemOpt.get().getResultJson().isBlank());
    }
}
