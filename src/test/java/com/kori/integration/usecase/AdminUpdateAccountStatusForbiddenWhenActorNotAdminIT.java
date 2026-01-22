package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.AccountEntity;
import com.kori.adapters.out.jpa.repo.AccountJpaRepository;
import com.kori.adapters.out.jpa.repo.AuditEventJpaRepository;
import com.kori.adapters.out.jpa.repo.IdempotencyJpaRepository;
import com.kori.application.command.AdminUpdateAccountStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.AdminUpdateAccountStatusUseCase;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.account.AdminAccountStatusAction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class AdminUpdateAccountStatusForbiddenWhenActorNotAdminIT {

    @Autowired AdminUpdateAccountStatusUseCase adminUpdateAccountStatusUseCase;

    @Autowired AccountJpaRepository accountJpaRepository;
    @Autowired AuditEventJpaRepository auditEventJpaRepository;
    @Autowired IdempotencyJpaRepository idempotencyJpaRepository;

    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void adminUpdateAccountStatus_isForbidden_whenActorIsNotAdmin() {
        // Given: create client + account
        UUID clientId = UUID.randomUUID();

        // Adapt columns if your clients table differs.
        // This assumes at least (id, phone_number, status) exist.
        jdbcTemplate.update(
                "insert into clients (id, phone_number, status) values (?, ?, ?)",
                clientId,
                "+269999" + (100000 + (int)(Math.random() * 899999)),
                "ACTIVE"
        );

        UUID accountId = UUID.randomUUID();
        accountJpaRepository.saveAndFlush(new AccountEntity(accountId, clientId, "ACTIVE"));

        String statusBefore = accountJpaRepository.findById(accountId)
                .orElseThrow(() -> new AssertionError("Account should exist"))
                .getStatus();

        long auditBefore = auditEventJpaRepository.count();
        long idemBefore = idempotencyJpaRepository.count();

        // When / Then: non-admin actor tries
        assertThrows(ForbiddenOperationException.class, () ->
                adminUpdateAccountStatusUseCase.execute(new AdminUpdateAccountStatusCommand(
                        "it-admin-account-status-forbidden-" + UUID.randomUUID(),
                        new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                        accountId.toString(),
                        AdminAccountStatusAction.SUSPENDED,
                        "not allowed"
                ))
        );

        // Then: status unchanged
        AccountEntity after = accountJpaRepository.findById(accountId)
                .orElseThrow(() -> new AssertionError("Account should exist"));
        assertEquals(statusBefore, after.getStatus(), "Account status must not change");

        // And: no side effects
        assertEquals(auditBefore, auditEventJpaRepository.count(), "No audit event should be written");
        assertEquals(idemBefore, idempotencyJpaRepository.count(), "No idempotency record should be written");
    }
}
