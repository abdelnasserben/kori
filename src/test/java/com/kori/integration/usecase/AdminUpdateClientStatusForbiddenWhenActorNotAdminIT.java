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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class AdminUpdateClientStatusForbiddenWhenActorNotAdminIT {

    @Autowired AdminUpdateClientStatusUseCase adminUpdateClientStatusUseCase;

    @Autowired AuditEventJpaRepository auditEventJpaRepository;
    @Autowired IdempotencyJpaRepository idempotencyJpaRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void adminUpdateClientStatus_isForbidden_whenActorIsNotAdmin() {
        // Given: client exists
        UUID clientId = UUID.randomUUID();
        String phone = "+269997" + (100000 + (int)(Math.random() * 899999));

        jdbcTemplate.update(
                "insert into clients (id, phone_number, status) values (?, ?, ?)",
                clientId,
                phone,
                "ACTIVE"
        );

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
                        "it-admin-client-status-forbidden-" + UUID.randomUUID(),
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
