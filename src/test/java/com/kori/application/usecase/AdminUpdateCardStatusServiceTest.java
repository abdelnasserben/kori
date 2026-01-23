package com.kori.application.usecase;

import com.kori.application.command.AdminUpdateCardStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.CardRepositoryPort;
import com.kori.application.port.out.IdempotencyPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.AdminUpdateCardStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.account.AccountId;
import com.kori.domain.model.card.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUpdateCardStatusServiceTest {

    @Mock TimeProviderPort timeProviderPort;
    @Mock IdempotencyPort idempotencyPort;
    @Mock CardRepositoryPort cardRepositoryPort;
    @Mock AuditPort auditPort;

    private AdminUpdateCardStatusService service;

    @BeforeEach
    void setUp() {
        service = new AdminUpdateCardStatusService(
                timeProviderPort, idempotencyPort, cardRepositoryPort, auditPort
        );
    }

    @Test
    void happyPath_setsSuspended_andAudits() {
        Instant now = Instant.parse("2026-01-21T10:15:30Z");
        when(timeProviderPort.now()).thenReturn(now);
        when(idempotencyPort.find("idem-1", AdminUpdateCardStatusResult.class)).thenReturn(Optional.empty());

        Card existing = new Card(
                CardId.of("card-1"),
                AccountId.of("acc-1"),
                "CARD-UID-1",
                new HashedPin("1234"),
                CardStatus.ACTIVE,
                0
        );
        when(cardRepositoryPort.findByCardUid("CARD-UID-1")).thenReturn(Optional.of(existing));
        when(cardRepositoryPort.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUpdateCardStatusCommand cmd = new AdminUpdateCardStatusCommand(
                "idem-1",
                new ActorContext(ActorType.ADMIN, "admin-actor", Map.of()),
                "CARD-UID-1",
                AdminCardStatusAction.SUSPENDED,
                "risk"
        );

        AdminUpdateCardStatusResult result = service.execute(cmd);

        assertEquals("SUSPENDED", result.status());

        ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepositoryPort).save(captor.capture());
        assertEquals(CardStatus.SUSPENDED, captor.getValue().status());

        verify(auditPort).publish(argThat(ev ->
                ev.action().equals("ADMIN_SET_CARD_STATUS_SUSPENDED")
                        && ev.actorType().equals("ADMIN")
                        && ev.actorId().equals("admin-actor")
                        && ev.occurredAt().equals(now)
                        && "CARD-UID-1".equals(ev.metadata().get("cardUid"))
        ));

        verify(idempotencyPort).save("idem-1", result);
    }

    @Test
    void forbidden_whenActorNotAdmin() {
        when(idempotencyPort.find("idem-2", AdminUpdateCardStatusResult.class)).thenReturn(Optional.empty());

        AdminUpdateCardStatusCommand cmd = new AdminUpdateCardStatusCommand(
                "idem-2",
                new ActorContext(ActorType.AGENT, "agent-actor", Map.of()),
                "CARD-UID-1",
                AdminCardStatusAction.INACTIVE,
                "reason"
        );

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd));
    }

    @Test
    void forbidden_adminUpdatingCardFromBlockedToActiveStatus() {
        Card blocked = new Card(
                CardId.of("card-1"),
                AccountId.of("acc-1"),
                "CARD-UID-1",
                new HashedPin("$hash"),
                CardStatus.BLOCKED,
                3
        );
        when(cardRepositoryPort.findByCardUid("CARD-UID-1")).thenReturn(Optional.of(blocked));

        AdminUpdateCardStatusCommand cmd = new AdminUpdateCardStatusCommand(
                "idem-2",
            new ActorContext(ActorType.ADMIN, "admin-1", Map.of()),
                "CARD-UID-1",
                AdminCardStatusAction.ACTIVE,
                "reason"
        );

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd));

        verify(cardRepositoryPort, never()).save(any());
        verify(auditPort, never()).publish(any());
    }

    @Test
    void forbidden_adminUpdatingCardFromBLockedToActiveStatus() {
        Card lost = new Card(
                CardId.of("card-1"),
                AccountId.of("acc-1"),
                "CARD-UID-1",
                new HashedPin("$hash"),
                CardStatus.LOST,
                0
        );
        when(cardRepositoryPort.findByCardUid("CARD-UID-1")).thenReturn(Optional.of(lost));

        AdminUpdateCardStatusCommand cmd = new AdminUpdateCardStatusCommand(
                "idem-1",
                new ActorContext(ActorType.ADMIN, "admin-1", Map.of()),
                "CARD-UID-1",
                AdminCardStatusAction.ACTIVE,
                "reason"
        );

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd));

        verify(cardRepositoryPort, never()).save(any());
        verify(auditPort, never()).publish(any());
    }

    @Test
    void allowsAdminUpdatingCardFromLostToInactiveStatus() {
        when(timeProviderPort.now()).thenReturn(Instant.parse("2026-01-21T10:00:00Z"));

        Card lost = new Card(
                CardId.of("card-1"),
                AccountId.of("acc-1"),
                "CARD-UID-1",
                new HashedPin("$hash"),
                CardStatus.LOST,
                0
        );
        when(cardRepositoryPort.findByCardUid("CARD-UID-1")).thenReturn(Optional.of(lost));

        AdminUpdateCardStatusCommand cmd = new AdminUpdateCardStatusCommand(
                "idem-2",
                new ActorContext(ActorType.ADMIN, "admin-1", Map.of()),
                "CARD-UID-1",
                AdminCardStatusAction.INACTIVE,
                "reason"
        );

        assertDoesNotThrow(() -> service.execute(cmd));

        verify(cardRepositoryPort).save(argThat(c -> c.status() == CardStatus.INACTIVE));
        verify(auditPort).publish(any());
    }

}
