package com.kori.application.usecase;

import com.kori.application.command.AdminUpdateCardStatusCommand;
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
final class AdminUpdateCardStatusServiceTest {

    // ======= mocks =======
    @Mock TimeProviderPort timeProviderPort;
    @Mock CardRepositoryPort cardRepositoryPort;
    @Mock AuditPort auditPort;

    @InjectMocks AdminUpdateCardStatusService service;

    // ======= constants =======
    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static final String ADMIN_ID = "admin.user";
    private static final String AGENT_ID = "A-000001";

    private static final String CARD_UID = "04A1B2C3D4E5F6A7B8C9D";
    private static final UUID CARD_ID_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CLIENT_ID_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final String REASON = "Ops action";

    // ======= helpers =======
    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, ADMIN_ID, Map.of());
    }

    private static ActorContext agentActor() {
        return new ActorContext(ActorType.AGENT, AGENT_ID, Map.of());
    }

    private static AdminUpdateCardStatusCommand cmd(ActorContext actor, String targetStatus) {
        return new AdminUpdateCardStatusCommand(actor, CARD_UID, targetStatus, REASON);
    }

    private static Card cardWithStatus(CardStatus status) {
        return new Card(
                new CardId(CARD_ID_UUID),
                new ClientId(CLIENT_ID_UUID),
                CARD_UID,
                new HashedPin("HASHED"),
                status,
                0,
                NOW
        );
    }

    // ======= tests =======

    @Test
    void forbidden_whenActorIsNotAdmin() {
        assertThrows(ForbiddenOperationException.class, () ->
                service.execute(cmd(agentActor(), CardStatus.SUSPENDED.name()))
        );

        verifyNoInteractions(timeProviderPort, cardRepositoryPort, auditPort);
    }

    @Test
    void forbidden_whenTargetStatusIsNotAllowed() {
        assertThrows(ForbiddenOperationException.class, () ->
                service.execute(cmd(adminActor(), CardStatus.BLOCKED.name()))
        );

        verifyNoInteractions(timeProviderPort, cardRepositoryPort, auditPort);
    }

    @Test
    void throwsNotFound_whenCardDoesNotExist() {
        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.execute(cmd(adminActor(), CardStatus.SUSPENDED.name()))
        );

        verify(cardRepositoryPort).findByCardUid(CARD_UID);
        verifyNoInteractions(timeProviderPort, auditPort);
        verify(cardRepositoryPort, never()).save(any());
    }

    @Test
    void happyPath_suspendsCard_saves_audits_andReturnsResult() {
        Card card = cardWithStatus(CardStatus.ACTIVE);

        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.of(card));
        when(cardRepositoryPort.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateCardStatusResult out = service.execute(cmd(adminActor(), CardStatus.SUSPENDED.name()));

        assertEquals(CARD_UID, out.cardUid());
        assertEquals(CardStatus.ACTIVE.name(), out.previousStatus());
        assertEquals(CardStatus.SUSPENDED.name(), out.newStatus());

        // domain mutation happened
        assertEquals(CardStatus.SUSPENDED, card.status());
        verify(cardRepositoryPort).save(card);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        AuditEvent event = auditCaptor.getValue();

        assertEquals("ADMIN_CARD_STATUS_UPDATED_" + CardStatus.SUSPENDED.name(), event.action());
        assertEquals("ADMIN", event.actorType());
        assertEquals(ADMIN_ID, event.actorRef());
        assertEquals(NOW, event.occurredAt());

        assertEquals(CARD_ID_UUID.toString(), event.metadata().get("cardId"));
        assertEquals(CardStatus.ACTIVE.name(), event.metadata().get("before"));
        assertEquals(CardStatus.SUSPENDED.name(), event.metadata().get("after"));
        assertEquals(CardStatus.SUSPENDED.name(), event.metadata().get("target"));
        assertEquals(REASON, event.metadata().get("reason"));
    }

    @Test
    void happyPath_deactivatesCard_saves_audits_andReturnsResult() {
        Card card = cardWithStatus(CardStatus.ACTIVE);

        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.of(card));
        when(cardRepositoryPort.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateCardStatusResult out = service.execute(cmd(adminActor(), CardStatus.INACTIVE.name()));

        assertEquals(CARD_UID, out.cardUid());
        assertEquals(CardStatus.ACTIVE.name(), out.previousStatus());
        assertEquals(CardStatus.INACTIVE.name(), out.newStatus());

        assertEquals(CardStatus.INACTIVE, card.status());
        verify(cardRepositoryPort).save(card);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        AuditEvent event = auditCaptor.getValue();

        assertEquals("ADMIN_CARD_STATUS_UPDATED_" + CardStatus.INACTIVE.name(), event.action());
        assertEquals(CardStatus.ACTIVE.name(), event.metadata().get("before"));
        assertEquals(CardStatus.INACTIVE.name(), event.metadata().get("after"));
    }

    @Test
    void throwsInvalidStatusTransition_whenActivatingBlockedCard() {
        Card card = cardWithStatus(CardStatus.BLOCKED);

        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.of(card));

        assertThrows(InvalidStatusTransitionException.class, () ->
                service.execute(cmd(adminActor(), CardStatus.ACTIVE.name()))
        );

        verify(cardRepositoryPort, never()).save(any());
        verifyNoInteractions(timeProviderPort, auditPort);
    }

    @Test
    void throwsInvalidStatusTransition_whenChangingLostCard() {
        Card card = cardWithStatus(CardStatus.LOST);

        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.of(card));

        assertThrows(InvalidStatusTransitionException.class, () ->
                service.execute(cmd(adminActor(), CardStatus.SUSPENDED.name()))
        );

        verify(cardRepositoryPort, never()).save(any());
        verifyNoInteractions(timeProviderPort, auditPort);
    }
}
