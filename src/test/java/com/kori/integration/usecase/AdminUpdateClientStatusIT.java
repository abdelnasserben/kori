package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.IdempotencyRecordEntity;
import com.kori.adapters.out.jpa.repo.AuditEventJpaRepository;
import com.kori.adapters.out.jpa.repo.IdempotencyJpaRepository;
import com.kori.application.command.AdminUpdateClientStatusCommand;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AdminUpdateClientStatusIT {

    @Autowired AdminUpdateClientStatusUseCase adminUpdateClientStatusUseCase;

    @Autowired AuditEventJpaRepository auditEventJpaRepository;
    @Autowired IdempotencyJpaRepository idempotencyJpaRepository;

    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void happyPath_adminUpdatesClientStatus_updatesDb_writesAudit_andIdempotency() {
        // Given: create client in DB
        UUID clientId = UUID.randomUUID();
        String phone = "+269998" + (100000 + (int)(Math.random() * 899999));

        // Adapt this insert to match your clients table constraints (reuse the one that already works in your project)
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
        assertNotNull(statusBefore);

        long auditBefore = auditEventJpaRepository.count();
        String idemKey = "it-admin-client-status-" + UUID.randomUUID();

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
