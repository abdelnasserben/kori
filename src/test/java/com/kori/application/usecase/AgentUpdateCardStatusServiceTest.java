package com.kori.application.usecase;

import com.kori.application.command.AgentUpdateCardStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.out.*;
import com.kori.application.result.AgentUpdateCardStatusResult;
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
class AgentUpdateCardStatusServiceTest {

    @Mock TimeProviderPort timeProviderPort;
    @Mock IdempotencyPort idempotencyPort;
    @Mock AgentRepositoryPort agentRepositoryPort;
    @Mock CardRepositoryPort cardRepositoryPort;
    @Mock AuditPort auditPort;

    private AgentUpdateCardStatusService service;

    @BeforeEach
    void setUp() {
        service = new AgentUpdateCardStatusService(
                timeProviderPort, idempotencyPort,
                agentRepositoryPort, cardRepositoryPort,
                auditPort
        );
    }

    @Test
    void returnsCachedResultWhenIdempotencyHit() {
        AgentUpdateCardStatusResult cached = new AgentUpdateCardStatusResult("card-1", "CARD-UID-1", "BLOCKED");
        when(idempotencyPort.find("idem-1", AgentUpdateCardStatusResult.class)).thenReturn(Optional.of(cached));

        AgentUpdateCardStatusCommand cmd = new AgentUpdateCardStatusCommand(
                "idem-1",
                new ActorContext(ActorType.AGENT, "agent-actor", Map.of()),
                "agent-1",
                "CARD-UID-1",
                AgentCardAction.BLOCKED,
                "reason"
        );

        AgentUpdateCardStatusResult result = service.execute(cmd);

        assertSame(cached, result);
        verifyNoInteractions(agentRepositoryPort, cardRepositoryPort, auditPort);
    }

    @Test
    void happyPath_blocksCard_andAuditsWithBlockAction() {
        Instant now = Instant.parse("2026-01-21T10:15:30Z");
        when(timeProviderPort.now()).thenReturn(now);

        when(idempotencyPort.find("idem-2", AgentUpdateCardStatusResult.class)).thenReturn(Optional.empty());
        when(agentRepositoryPort.existsById("agent-1")).thenReturn(true);

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

        AgentUpdateCardStatusCommand cmd = new AgentUpdateCardStatusCommand(
                "idem-2",
                new ActorContext(ActorType.AGENT, "agent-actor", Map.of()),
                "agent-1",
                "CARD-UID-1",
                AgentCardAction.BLOCKED,
                "client request"
        );

        AgentUpdateCardStatusResult result = service.execute(cmd);

        assertEquals("BLOCKED", result.status());

        ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepositoryPort).save(cardCaptor.capture());
        assertEquals(CardStatus.BLOCKED, cardCaptor.getValue().status());

        verify(auditPort).publish(argThat(ev ->
                ev.action().equals("AGENT_BLOCK_CARD")
                        && ev.actorType().equals("AGENT")
                        && ev.actorId().equals("agent-actor")
                        && ev.occurredAt().equals(now)
                        && "agent-1".equals(ev.metadata().get("agentId"))
                        && "CARD-UID-1".equals(ev.metadata().get("cardUid"))
        ));

        verify(idempotencyPort).save("idem-2", result);
    }

    @Test
    void happyPath_marksLost_andAuditsWithLostAction() {
        Instant now = Instant.parse("2026-01-21T10:15:30Z");
        when(timeProviderPort.now()).thenReturn(now);

        when(idempotencyPort.find("idem-3", AgentUpdateCardStatusResult.class)).thenReturn(Optional.empty());
        when(agentRepositoryPort.existsById("agent-1")).thenReturn(true);

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

        AgentUpdateCardStatusCommand cmd = new AgentUpdateCardStatusCommand(
                "idem-3",
                new ActorContext(ActorType.AGENT, "agent-actor", Map.of()),
                "agent-1",
                "CARD-UID-1",
                AgentCardAction.LOST,
                "lost by client"
        );

        AgentUpdateCardStatusResult result = service.execute(cmd);

        assertEquals("LOST", result.status());

        ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepositoryPort).save(cardCaptor.capture());
        assertEquals(CardStatus.LOST, cardCaptor.getValue().status());

        verify(auditPort).publish(argThat(ev ->
                ev.action().equals("AGENT_MARK_CARD_LOST")
                        && ev.actorType().equals("AGENT")
                        && ev.actorId().equals("agent-actor")
                        && ev.occurredAt().equals(now)
                        && "agent-1".equals(ev.metadata().get("agentId"))
                        && "CARD-UID-1".equals(ev.metadata().get("cardUid"))
        ));

        verify(idempotencyPort).save("idem-3", result);
    }

    @Test
    void forbidden_whenActorNotAgent() {
        when(idempotencyPort.find("idem-4", AgentUpdateCardStatusResult.class)).thenReturn(Optional.empty());

        AgentUpdateCardStatusCommand cmd = new AgentUpdateCardStatusCommand(
                "idem-4",
                new ActorContext(ActorType.ADMIN, "admin-actor", Map.of()),
                "agent-1",
                "CARD-UID-1",
                AgentCardAction.BLOCKED,
                "reason"
        );

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd));
    }
}
