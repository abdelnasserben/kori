package com.kori.application.usecase;

import com.kori.application.command.AdminUnblockCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.CardRepositoryPort;
import com.kori.application.port.out.IdempotencyPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.AdminUnblockCardResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.account.AccountId;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardId;
import com.kori.domain.model.card.CardStatus;
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
class AdminUnblockCardServiceTest {

    @Mock TimeProviderPort timeProviderPort;
    @Mock IdempotencyPort idempotencyPort;
    @Mock CardRepositoryPort cardRepositoryPort;
    @Mock AuditPort auditPort;

    private AdminUnblockCardService service;

    @BeforeEach
    void setUp() {
        service = new AdminUnblockCardService(
                timeProviderPort, idempotencyPort,
                cardRepositoryPort, auditPort
        );
    }

    @Test
    void returnsCachedResultWhenIdempotencyHit() {
        AdminUnblockCardResult cached = new AdminUnblockCardResult("card-1", "CARD-UID-1", "ACTIVE", 0);
        when(idempotencyPort.find("idem-1", AdminUnblockCardResult.class)).thenReturn(Optional.of(cached));

        AdminUnblockCardCommand cmd = new AdminUnblockCardCommand(
                "idem-1",
                new ActorContext(ActorType.ADMIN, "admin-actor", Map.of()),
                "CARD-UID-1",
                "reason"
        );

        AdminUnblockCardResult result = service.execute(cmd);

        assertSame(cached, result);
        verifyNoInteractions(cardRepositoryPort, auditPort);
    }

    @Test
    void happyPath_unblocksAndResetsAttempts_andAudits() {
        Instant now = Instant.parse("2026-01-21T10:15:30Z");
        when(timeProviderPort.now()).thenReturn(now);

        when(idempotencyPort.find("idem-2", AdminUnblockCardResult.class)).thenReturn(Optional.empty());

        Card blocked = new Card(
                CardId.of("card-1"),
                AccountId.of("acc-1"),
                "CARD-UID-1",
                "1234",
                CardStatus.BLOCKED,
                3
        );
        when(cardRepositoryPort.findByCardUid("CARD-UID-1")).thenReturn(Optional.of(blocked));
        when(cardRepositoryPort.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUnblockCardCommand cmd = new AdminUnblockCardCommand(
                "idem-2",
                new ActorContext(ActorType.ADMIN, "admin-actor", Map.of()),
                "CARD-UID-1",
                "manual unblock"
        );

        AdminUnblockCardResult result = service.execute(cmd);

        assertEquals("ACTIVE", result.status());
        assertEquals(0, result.failedPinAttempts());

        ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepositoryPort).save(captor.capture());
        Card saved = captor.getValue();
        assertEquals(CardStatus.ACTIVE, saved.status());
        assertEquals(0, saved.failedPinAttempts());

        verify(auditPort).publish(argThat(ev ->
                ev.action().equals("ADMIN_UNBLOCK_CARD")
                        && ev.actorType().equals("ADMIN")
                        && ev.actorId().equals("admin-actor")
                        && ev.occurredAt().equals(now)
                        && "CARD-UID-1".equals(ev.metadata().get("cardUid"))
        ));

        verify(idempotencyPort).save("idem-2", result);
    }

    @Test
    void forbidden_whenActorNotAdmin() {
        when(idempotencyPort.find("idem-3", AdminUnblockCardResult.class)).thenReturn(Optional.empty());

        AdminUnblockCardCommand cmd = new AdminUnblockCardCommand(
                "idem-3",
                new ActorContext(ActorType.AGENT, "agent-actor", Map.of()),
                "CARD-UID-1",
                "reason"
        );

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd));
    }
}
