package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.CardEntity;
import com.kori.adapters.out.jpa.repo.AuditEventJpaRepository;
import com.kori.adapters.out.jpa.repo.CardJpaRepository;
import com.kori.adapters.out.jpa.repo.IdempotencyJpaRepository;
import com.kori.application.command.AgentUpdateCardStatusCommand;
import com.kori.application.command.EnrollCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class AgentUpdateCardStatusForbiddenWhenActorNotAgentIT {

    @Autowired EnrollCardUseCase enrollCardUseCase;
    @Autowired AgentUpdateCardStatusUseCase agentUpdateCardStatusUseCase;

    @Autowired CardJpaRepository cardJpaRepository;
    @Autowired AuditEventJpaRepository auditEventJpaRepository;
    @Autowired IdempotencyJpaRepository idempotencyJpaRepository;

    @Test
    void agentUpdateCardStatus_isForbidden_whenActorIsNotAgent() {
        // Given: a card exists
        String agentId = "AGENT_001";
        String phoneNumber = "+269870" + (100000 + (int) (Math.random() * 899999));
        String cardUid = "CARD-" + UUID.randomUUID();

        enrollCardUseCase.execute(new EnrollCardCommand(
                "it-enroll-for-status-forbidden-" + UUID.randomUUID(),
                new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                phoneNumber,
                cardUid,
                "1234",
                agentId
        ));

        CardEntity before = cardJpaRepository.findByCardUid(cardUid)
                .orElseThrow(() -> new AssertionError("Card should exist"));
        String statusBefore = before.getStatus();

        long auditBefore = auditEventJpaRepository.count();
        long idemBefore = idempotencyJpaRepository.count();

        // When / Then: ADMIN tries an AGENT-only action
        assertThrows(ForbiddenOperationException.class, () ->
                agentUpdateCardStatusUseCase.execute(new AgentUpdateCardStatusCommand(
                        "it-status-admin-forbidden-" + UUID.randomUUID(),
                        new ActorContext(ActorType.ADMIN, "admin-actor-it", Map.of()),
                        agentId,
                        cardUid,
                        AgentCardAction.BLOCKED,
                        "attempt by non-agent"
                ))
        );

        // And: card status unchanged
        CardEntity after = cardJpaRepository.findByCardUid(cardUid).orElseThrow();
        assertEquals(statusBefore, after.getStatus(), "Card status must not change");

        // And: no side effects
        assertEquals(auditBefore, auditEventJpaRepository.count(), "No audit event should be created");
        assertEquals(idemBefore, idempotencyJpaRepository.count(), "No idempotency record should be created");
    }
}
