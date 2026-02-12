package com.kori.application.usecase;

import com.kori.application.command.AgentUpdateCardStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.out.AgentRepositoryPort;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.CardRepositoryPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateCardStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.common.InvalidStatusTransitionException;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.agent.AgentId;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardId;
import com.kori.domain.model.card.CardStatus;
import com.kori.domain.model.card.HashedPin;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class AgentUpdateCardStatusServiceTest {

    // ======= mocks =======
    @Mock TimeProviderPort timeProviderPort;
    @Mock AgentRepositoryPort agentRepositoryPort;
    @Mock CardRepositoryPort cardRepositoryPort;
    @Mock AuditPort auditPort;

    @InjectMocks AgentUpdateCardStatusService service;

    // ======= constants (single source of truth) =======
    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static final String ACTOR_ID = "agent-actor";
    private static final String ADMIN_ID = "admin-actor";

    private static final String AGENT_CODE_RAW = "A-123456";
    private static final AgentCode AGENT_CODE = AgentCode.of(AGENT_CODE_RAW);

    private static final UUID AGENT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final String CARD_UID = "04A1B2C3D4E5F6A7B8C9D";
    private static final UUID CARD_ID_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CLIENT_UUID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final String REASON = "Customer request";

    // ======= helpers =======
    private static ActorContext agentActor() {
        return new ActorContext(ActorType.AGENT, ACTOR_ID, Map.of());
    }

    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, ADMIN_ID, Map.of());
    }

    private static Agent activeAgent() {
        return new Agent(new AgentId(AGENT_UUID), AGENT_CODE, NOW.minusSeconds(60), Status.ACTIVE);
    }

    private static Agent suspendedAgent() {
        return new Agent(new AgentId(AGENT_UUID), AGENT_CODE, NOW.minusSeconds(60), Status.SUSPENDED);
    }

    private static Card cardWithStatus(CardStatus status) {
        return new Card(
                new CardId(CARD_ID_UUID),
                new ClientId(CLIENT_UUID),
                CARD_UID,           // IMPORTANT: repo lookup uses CARD_UID.toString()
                new HashedPin("HASHED"),
                status,
                0,
                NOW
        );
    }

    private static AgentUpdateCardStatusCommand cmd(ActorContext actor, String targetStatus) {
        return new AgentUpdateCardStatusCommand(
                actor,
                CARD_UID,
                AGENT_CODE_RAW,
                targetStatus,
                REASON
        );
    }

    // ======= tests =======

    @Test
    void forbidden_whenActorIsNotAgent() {
        assertThrows(ForbiddenOperationException.class, () ->
                service.execute(cmd(adminActor(), CardStatus.BLOCKED.name()))
        );

        verifyNoInteractions(timeProviderPort, agentRepositoryPort, cardRepositoryPort, auditPort);
    }

    @Test
    void forbidden_whenTargetStatusIsNotBlockedOrLost() {
        assertThrows(ForbiddenOperationException.class, () ->
                service.execute(cmd(agentActor(), CardStatus.ACTIVE.name()))
        );

        // validation happens before any repo call
        verifyNoInteractions(timeProviderPort, agentRepositoryPort, cardRepositoryPort, auditPort);
    }

    @Test
    void throwsNotFound_whenAgentDoesNotExist() {
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.execute(cmd(agentActor(), CardStatus.BLOCKED.name()))
        );

        verify(agentRepositoryPort).findByCode(AGENT_CODE);
        verifyNoInteractions(timeProviderPort, cardRepositoryPort, auditPort);
    }

    @Test
    void forbidden_whenAgentIsNotActive() {
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(suspendedAgent()));

        assertThrows(ForbiddenOperationException.class, () ->
                service.execute(cmd(agentActor(), CardStatus.BLOCKED.name()))
        );

        verify(agentRepositoryPort).findByCode(AGENT_CODE);
        verifyNoInteractions(timeProviderPort, cardRepositoryPort, auditPort);
    }

    @Test
    void throwsNotFound_whenCardDoesNotExist() {
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(activeAgent()));
        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.execute(cmd(agentActor(), CardStatus.BLOCKED.name()))
        );

        verify(agentRepositoryPort).findByCode(AGENT_CODE);
        verify(cardRepositoryPort).findByCardUid(CARD_UID);
        verifyNoInteractions(timeProviderPort, auditPort);
        verify(cardRepositoryPort, never()).save(any());
    }

    @Test
    void happyPath_blocksCard_saves_andAudits_andReturnsResult() {
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(activeAgent()));

        Card card = cardWithStatus(CardStatus.ACTIVE);
        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.of(card));
        when(cardRepositoryPort.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateCardStatusResult out = service.execute(cmd(agentActor(), CardStatus.BLOCKED.name()));

        assertEquals(CARD_UID, out.cardUid());
        assertEquals(CardStatus.ACTIVE.name(), out.previousStatus());
        assertEquals(CardStatus.BLOCKED.name(), out.newStatus());

        verify(cardRepositoryPort).save(card);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        AuditEvent event = auditCaptor.getValue();

        assertEquals("AGENT_BLOCK_CARD", event.action());
        assertEquals("AGENT", event.actorType());
        assertEquals(ACTOR_ID, event.actorId());
        assertEquals(NOW, event.occurredAt());

        assertEquals(CARD_ID_UUID.toString(), event.metadata().get("cardId"));
        assertEquals(CardStatus.ACTIVE.name(), event.metadata().get("before"));
        assertEquals(CardStatus.BLOCKED.name(), event.metadata().get("after"));
        assertEquals(REASON, event.metadata().get("reason"));
    }

    @Test
    void happyPath_marksCardLost_saves_andAudits_andReturnsResult() {
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(activeAgent()));

        Card card = cardWithStatus(CardStatus.ACTIVE);
        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.of(card));
        when(cardRepositoryPort.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateCardStatusResult out = service.execute(cmd(agentActor(), CardStatus.LOST.name()));

        assertEquals(CARD_UID, out.cardUid());
        assertEquals(CardStatus.ACTIVE.name(), out.previousStatus());
        assertEquals(CardStatus.LOST.name(), out.newStatus());

        verify(cardRepositoryPort).save(card);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        AuditEvent event = auditCaptor.getValue();

        assertEquals("AGENT_MARK_CARD_LOST", event.action());
        assertEquals("AGENT", event.actorType());
        assertEquals(ACTOR_ID, event.actorId());
        assertEquals(NOW, event.occurredAt());

        assertEquals(CARD_ID_UUID.toString(), event.metadata().get("cardId"));
        assertEquals(CardStatus.ACTIVE.name(), event.metadata().get("before"));
        assertEquals(CardStatus.LOST.name(), event.metadata().get("after"));
        assertEquals(REASON, event.metadata().get("reason"));
    }

    @Test
    void throwsInvalidStatusTransition_whenBlockingLostCard() {
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(activeAgent()));

        Card card = cardWithStatus(CardStatus.LOST);
        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.of(card));

        assertThrows(InvalidStatusTransitionException.class, () ->
                service.execute(cmd(agentActor(), CardStatus.BLOCKED.name()))
        );

        // domain throws before save/audit
        verify(cardRepositoryPort, never()).save(any());
        verifyNoInteractions(timeProviderPort, auditPort);
    }
}
