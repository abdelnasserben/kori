package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.CardEntity;
import com.kori.adapters.out.jpa.entity.IdempotencyRecordEntity;
import com.kori.adapters.out.jpa.repo.AuditEventJpaRepository;
import com.kori.adapters.out.jpa.repo.CardJpaRepository;
import com.kori.adapters.out.jpa.repo.IdempotencyJpaRepository;
import com.kori.application.command.AdminUnblockCardCommand;
import com.kori.application.command.EnrollCardCommand;
import com.kori.application.port.in.AdminUnblockCardUseCase;
import com.kori.application.port.in.EnrollCardUseCase;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
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
class AdminUnblockCardIT {

    @Autowired EnrollCardUseCase enrollCardUseCase;
    @Autowired AdminUnblockCardUseCase adminUnblockCardUseCase;

    @Autowired CardJpaRepository cardJpaRepository;
    @Autowired AuditEventJpaRepository auditEventJpaRepository;
    @Autowired IdempotencyJpaRepository idempotencyJpaRepository;

    @Test
    void happyPath_adminUnblocksCard_updatesStatus_writesAudit_andIdempotency() {
        // Given: card exists and is BLOCKED
        String agentId = "AGENT_001";
        String phoneNumber = "+269900" + (100000 + (int) (Math.random() * 899999));
        String cardUid = "CARD-" + UUID.randomUUID();

        enrollCardUseCase.execute(new EnrollCardCommand(
                "it-enroll-for-admin-unblock-" + UUID.randomUUID(),
                new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                phoneNumber,
                cardUid,
                "1234",
                agentId
        ));

        CardEntity card = cardJpaRepository.findByCardUid(cardUid)
                .orElseThrow(() -> new AssertionError("Card should exist"));
        card.setStatus("BLOCKED");
        cardJpaRepository.saveAndFlush(card);

        long auditBefore = auditEventJpaRepository.count();
        String idemKey = "it-admin-unblock-" + UUID.randomUUID();

        // When: ADMIN unblocks
        adminUnblockCardUseCase.execute(new AdminUnblockCardCommand(
                idemKey,
                new ActorContext(ActorType.ADMIN, "admin-actor-it", Map.of()),
                cardUid,
                "manual review passed"
        ));

        // Then: status ACTIVE
        CardEntity after = cardJpaRepository.findByCardUid(cardUid).orElseThrow();
        assertEquals("ACTIVE", after.getStatus(), "Card status should become ACTIVE");

        // Then: audit +1
        assertEquals(auditBefore + 1, auditEventJpaRepository.count(), "One audit event should be written");

        // Then: idempotency exists
        Optional<IdempotencyRecordEntity> idemOpt = idempotencyJpaRepository.findById(idemKey);
        assertTrue(idemOpt.isPresent(), "Idempotency record should exist");
        assertNotNull(idemOpt.get().getResultJson());
        assertFalse(idemOpt.get().getResultJson().isBlank());
    }
}
