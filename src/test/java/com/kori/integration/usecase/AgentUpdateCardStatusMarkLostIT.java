package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.CardEntity;
import com.kori.adapters.out.jpa.entity.IdempotencyRecordEntity;
import com.kori.adapters.out.jpa.repo.AuditEventJpaRepository;
import com.kori.adapters.out.jpa.repo.CardJpaRepository;
import com.kori.adapters.out.jpa.repo.IdempotencyJpaRepository;
import com.kori.application.command.AgentUpdateCardStatusCommand;
import com.kori.application.command.EnrollCardCommand;
import com.kori.application.port.in.AgentUpdateCardStatusUseCase;
import com.kori.application.port.in.EnrollCardUseCase;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.card.AgentCardAction;
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
class AgentUpdateCardStatusMarkLostIT {

    @Autowired EnrollCardUseCase enrollCardUseCase;
    @Autowired AgentUpdateCardStatusUseCase agentUpdateCardStatusUseCase;

    @Autowired CardJpaRepository cardJpaRepository;
    @Autowired AuditEventJpaRepository auditEventJpaRepository;
    @Autowired IdempotencyJpaRepository idempotencyJpaRepository;

    @Test
    void happyPath_agentMarksCardLost_updatesStatus_writesAudit_andIdempotency() {
        // Given: active card exists
        String agentId = "AGENT_001";
        String phoneNumber = "+269890" + (100000 + (int) (Math.random() * 899999));
        String cardUid = "CARD-" + UUID.randomUUID();

        enrollCardUseCase.execute(new EnrollCardCommand(
                "it-enroll-for-lost-happy-" + UUID.randomUUID(),
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
        String idemKey = "it-agent-lost-" + UUID.randomUUID();

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
