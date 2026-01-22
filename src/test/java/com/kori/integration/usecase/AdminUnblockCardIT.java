package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.CardEntity;
import com.kori.adapters.out.jpa.entity.IdempotencyRecordEntity;
import com.kori.application.command.AdminUnblockCardCommand;
import com.kori.application.command.EnrollCardCommand;
import com.kori.application.port.in.AdminUnblockCardUseCase;
import com.kori.application.port.in.EnrollCardUseCase;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AdminUnblockCardIT extends AbstractIntegrationTest {

    @Autowired EnrollCardUseCase enrollCardUseCase;
    @Autowired AdminUnblockCardUseCase adminUnblockCardUseCase;

    @Test
    void happyPath_adminUnblocksCard_updatesStatus_writesAudit_andIdempotency() {
        // Given: card exists and is BLOCKED
        String agentId = "AGENT_001";
        String phoneNumber = randomPhone269();
        String cardUid = randomCardUid();

        enrollCardUseCase.execute(new EnrollCardCommand(
                idemKey("it-enroll-for-admin-unblock"),
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
        String idemKey = idemKey("it-admin-unblock");

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
