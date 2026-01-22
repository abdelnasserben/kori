package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.CardEntity;
import com.kori.adapters.out.jpa.entity.IdempotencyRecordEntity;
import com.kori.application.command.AgentUpdateCardStatusCommand;
import com.kori.application.command.EnrollCardCommand;
import com.kori.application.port.in.AgentUpdateCardStatusUseCase;
import com.kori.application.port.in.EnrollCardUseCase;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.card.AgentCardAction;
import com.kori.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AgentUpdateCardStatusMarkLostIT extends AbstractIntegrationTest {

    @Autowired EnrollCardUseCase enrollCardUseCase;
    @Autowired AgentUpdateCardStatusUseCase agentUpdateCardStatusUseCase;

    @Test
    void happyPath_agentMarksCardLost_updatesStatus_writesAudit_andIdempotency() {
        // Given: active card exists
        String agentId = "AGENT_001";
        String phoneNumber = randomPhone269();
        String cardUid = randomCardUid();

        enrollCardUseCase.execute(new EnrollCardCommand(
                idemKey("it-enroll-for-lost-happy"),
                new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                phoneNumber,
                cardUid,
                "1234",
                agentId
        ));

        CardEntity before = cardJpaRepository.findByCardUid(cardUid)
                .orElseThrow(() -> new AssertionError("Card should exist"));
        assertEquals("ACTIVE", before.getStatus(), "Precondition: card should be ACTIVE");

        long auditBefore = auditEventJpaRepository.count();
        String idemKey = idemKey("it-agent-lost");

        // When: agent marks card as LOST
        agentUpdateCardStatusUseCase.execute(new AgentUpdateCardStatusCommand(
                idemKey,
                new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                agentId,
                cardUid,
                AgentCardAction.LOST,
                "client reported lost"
        ));

        // Then: status changed
        CardEntity after = cardJpaRepository.findByCardUid(cardUid).orElseThrow();
        assertEquals("LOST", after.getStatus(), "Card status should become LOST");

        // Then: audit written
        assertEquals(auditBefore + 1, auditEventJpaRepository.count(), "One audit event should be written");

        // Then: idempotency written
        Optional<IdempotencyRecordEntity> idemOpt = idempotencyJpaRepository.findById(idemKey);
        assertTrue(idemOpt.isPresent(), "Idempotency record should exist");
        assertNotNull(idemOpt.get().getResultJson());
        assertFalse(idemOpt.get().getResultJson().isBlank());
    }
}
