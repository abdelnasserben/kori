package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.CardEntity;
import com.kori.application.command.AgentUpdateCardStatusCommand;
import com.kori.application.command.EnrollCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.AgentUpdateCardStatusUseCase;
import com.kori.application.port.in.EnrollCardUseCase;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.card.AgentCardAction;
import com.kori.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentUpdateCardStatusForbiddenWhenActorNotAgentIT extends AbstractIntegrationTest {

    @Autowired EnrollCardUseCase enrollCardUseCase;
    @Autowired AgentUpdateCardStatusUseCase agentUpdateCardStatusUseCase;

    @Test
    void agentUpdateCardStatus_isForbidden_whenActorIsNotAgent() {
        // Given: a card exists
        String agentId = "AGENT_001";
        String phoneNumber = randomPhone269();
        String cardUid = randomCardUid();

        enrollCardUseCase.execute(new EnrollCardCommand(
                idemKey("it-enroll-for-status-forbidden"),
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
                        idemKey("it-status-admin-forbidden"),
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
