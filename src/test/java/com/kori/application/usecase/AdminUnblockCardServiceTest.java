package com.kori.application.usecase;

import com.kori.application.command.AdminUnblockCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.CardRepositoryPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateCardStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.common.InvalidStatusTransitionException;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardId;
import com.kori.domain.model.card.CardStatus;
import com.kori.domain.model.card.HashedPin;
import com.kori.domain.model.client.ClientId;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class AdminUnblockCardServiceTest {

    // ======= mocks =======
    @Mock TimeProviderPort timeProviderPort;
    @Mock CardRepositoryPort cardRepositoryPort;
    @Mock AuditPort auditPort;

    @InjectMocks AdminUnblockCardService service;

    // ======= constants =======
    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static final String ADMIN_ID = "admin-actor";
    private static final String AGENT_ID = "agent-actor";

    private static final UUID CARD_UID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CARD_ID_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CLIENT_ID_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final String REASON = "Reset after pin failures";

    // ======= helpers =======
    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, ADMIN_ID, Map.of());
    }

    private static ActorContext agentActor() {
        return new ActorContext(ActorType.AGENT, AGENT_ID, Map.of());
    }

    private static AdminUnblockCardCommand cmd(ActorContext actor, String reason) {
        return new AdminUnblockCardCommand(actor, CARD_UID, reason);
    }

    private static Card card(CardStatus status, int failedAttempts) {
        return new Card(
                new CardId(CARD_ID_UUID),
                new ClientId(CLIENT_ID_UUID),
                CARD_UID.toString(),
                new HashedPin("HASHED"),
                status,
                failedAttempts
        );
    }

    // ======= tests =======

    @Test
    void forbidden_whenActorIsNotAdmin() {
        assertThrows(ForbiddenOperationException.class, () ->
                service.execute(cmd(agentActor(), REASON))
        );

        verifyNoInteractions(timeProviderPort, cardRepositoryPort, auditPort);
    }

    @Test
    void throwsNotFound_whenCardDoesNotExist() {
        when(cardRepositoryPort.findByCardUid(CARD_UID.toString())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.execute(cmd(adminActor(), REASON))
        );

        verify(cardRepositoryPort).findByCardUid(CARD_UID.toString());
        verifyNoInteractions(timeProviderPort, auditPort);
        verify(cardRepositoryPort, never()).save(any(Card.class));
    }

    @Test
    void throwsInvalidStatusTransition_whenCardIsNotBlocked() {
        Card active = card(CardStatus.ACTIVE, 2);

        when(cardRepositoryPort.findByCardUid(CARD_UID.toString())).thenReturn(Optional.of(active));

        assertThrows(InvalidStatusTransitionException.class, () ->
                service.execute(cmd(adminActor(), REASON))
        );

        verify(cardRepositoryPort, never()).save(any(Card.class));
        verifyNoInteractions(timeProviderPort, auditPort);
    }

    @Test
    void happyPath_unblocksBlockedCard_resetsPinAttempts_saves_audits_andReturnsResult() {
        Card blocked = card(CardStatus.BLOCKED, 3);

        when(cardRepositoryPort.findByCardUid(CARD_UID.toString())).thenReturn(Optional.of(blocked));
        when(cardRepositoryPort.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateCardStatusResult out = service.execute(cmd(adminActor(), REASON));

        // result
        assertEquals(CARD_UID, out.cardUid());
        assertEquals(CardStatus.BLOCKED.name(), out.previousStatus());
        assertEquals(CardStatus.ACTIVE.name(), out.newStatus());

        // domain state changed
        assertEquals(CardStatus.ACTIVE, blocked.status());
        assertEquals(0, blocked.failedPinAttempts());

        verify(cardRepositoryPort).save(blocked);

        // audit
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        AuditEvent event = auditCaptor.getValue();

        assertEquals("ADMIN_UNBLOCK_CARD", event.action());
        assertEquals("ADMIN", event.actorType());
        assertEquals(ADMIN_ID, event.actorId());
        assertEquals(NOW, event.occurredAt());

        assertEquals(CARD_ID_UUID.toString(), event.metadata().get("cardId"));
        assertEquals(CardStatus.BLOCKED.name(), event.metadata().get("before"));
        assertEquals(CardStatus.ACTIVE.name(), event.metadata().get("after"));
        assertEquals(REASON, event.metadata().get("reason"));
    }

    @Test
    void reasonDefaultsToNA_whenBlank() {
        Card blocked = card(CardStatus.BLOCKED, 1);

        when(cardRepositoryPort.findByCardUid(CARD_UID.toString())).thenReturn(Optional.of(blocked));
        when(cardRepositoryPort.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
        when(timeProviderPort.now()).thenReturn(NOW);

        service.execute(cmd(adminActor(), "   "));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());

        assertEquals("N/A", auditCaptor.getValue().metadata().get("reason"));
    }
}
