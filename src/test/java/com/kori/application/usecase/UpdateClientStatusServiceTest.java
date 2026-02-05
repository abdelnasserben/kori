package com.kori.application.usecase;

import com.kori.application.command.UpdateClientStatusCommand;
import com.kori.application.events.ClientStatusChangedEvent;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.out.*;
import com.kori.application.result.UpdateClientStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.common.InvalidStatusTransitionException;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.common.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class UpdateClientStatusServiceTest {

    // ======= mocks =======
    @Mock ClientRepositoryPort clientRepositoryPort;
    @Mock AuditPort auditPort;
    @Mock TimeProviderPort timeProviderPort;
    @Mock DomainEventPublisherPort domainEventPublisherPort;
    @Mock LedgerQueryPort ledgerQueryPort;

    @InjectMocks UpdateClientStatusService service;

    // ======= constants =======
    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static final String ADMIN_ID = "admin-actor";
    private static final String AGENT_ID = "agent-actor";

    private static final ClientId CLIENT_ID = new ClientId(UUID.randomUUID());
    private static final String CLIENT_ID_RAW = CLIENT_ID.value().toString();

    private static final String REASON = "Ops action";

    // ======= helpers =======
    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, ADMIN_ID, Map.of());
    }

    private static ActorContext nonAdminActor() {
        return new ActorContext(ActorType.AGENT, AGENT_ID, Map.of());
    }

    private static UpdateClientStatusCommand cmd(ActorContext actor, String targetStatus, String reason) {
        return new UpdateClientStatusCommand(actor, CLIENT_ID_RAW, targetStatus, reason);
    }

    private static Client clientWithStatus(Status status) {
        return new Client(ClientId.of(CLIENT_ID_RAW), "+26912345678", status, NOW);
    }

    // ======= tests =======

    @Test
    void forbidden_whenActorIsNotAdmin() {
        assertThrows(ForbiddenOperationException.class, () ->
                service.execute(cmd(nonAdminActor(), Status.SUSPENDED.name(), REASON))
        );

        verifyNoInteractions(clientRepositoryPort, auditPort, timeProviderPort, domainEventPublisherPort);
    }

    @Test
    void throwsNotFound_whenClientDoesNotExist() {
        when(clientRepositoryPort.findById(ClientId.of(CLIENT_ID_RAW))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.execute(cmd(adminActor(), Status.SUSPENDED.name(), REASON))
        );

        verify(clientRepositoryPort).findById(ClientId.of(CLIENT_ID_RAW));
        verifyNoInteractions(auditPort, timeProviderPort, domainEventPublisherPort);
        verify(clientRepositoryPort, never()).save(any(Client.class));
    }

    @Test
    void happyPath_suspendsClient_saves_audits_publishesEvent_andReturnsResult() {
        Client client = clientWithStatus(Status.ACTIVE);

        when(clientRepositoryPort.findById(ClientId.of(CLIENT_ID_RAW))).thenReturn(Optional.of(client));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateClientStatusResult out = service.execute(cmd(adminActor(), Status.SUSPENDED.name(), REASON));

        assertEquals(CLIENT_ID_RAW, out.clientId());
        assertEquals(Status.ACTIVE.name(), out.previousStatus());
        assertEquals(Status.SUSPENDED.name(), out.newStatus());

        assertEquals(Status.SUSPENDED, client.status());
        verify(clientRepositoryPort).save(client);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        AuditEvent event = auditCaptor.getValue();

        assertEquals("ADMIN_UPDATE_CLIENT_STATUS_" + Status.SUSPENDED.name(), event.action());
        assertEquals(ActorType.ADMIN.name(), event.actorType());
        assertEquals(ADMIN_ID, event.actorId());
        assertEquals(NOW, event.occurredAt());

        assertEquals(CLIENT_ID_RAW, event.metadata().get("clientId"));
        assertEquals(Status.ACTIVE.name(), event.metadata().get("before"));
        assertEquals(Status.SUSPENDED.name(), event.metadata().get("after"));
        assertEquals(REASON, event.metadata().get("reason"));

        verify(domainEventPublisherPort).publish(any(ClientStatusChangedEvent.class));
    }

    @Test
    void happyPath_activatesClient_saves_audits_publishesEvent_andReturnsResult() {
        Client client = clientWithStatus(Status.SUSPENDED);

        when(clientRepositoryPort.findById(ClientId.of(CLIENT_ID_RAW))).thenReturn(Optional.of(client));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateClientStatusResult out = service.execute(cmd(adminActor(), Status.ACTIVE.name(), REASON));

        assertEquals(Status.SUSPENDED.name(), out.previousStatus());
        assertEquals(Status.ACTIVE.name(), out.newStatus());

        assertEquals(Status.ACTIVE, client.status());
        verify(clientRepositoryPort).save(client);
        verify(auditPort).publish(any(AuditEvent.class));
        verify(domainEventPublisherPort).publish(any(ClientStatusChangedEvent.class));
    }

    @Test
    void throws_whenClosingClientWithNonZeroWalletBalance() {
        Client client = clientWithStatus(Status.SUSPENDED);

        when(clientRepositoryPort.findById(ClientId.of(CLIENT_ID_RAW))).thenReturn(Optional.of(client));
        when(ledgerQueryPort.netBalance(LedgerAccountRef.client(CLIENT_ID_RAW))).thenReturn(Money.of(BigDecimal.ONE));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.execute(cmd(adminActor(), Status.CLOSED.name(), REASON))
        );

        assertEquals("CLIENT_WALLET_BALANCE_MUST_BE_ZERO_TO_CLOSE", ex.getMessage());
        verify(clientRepositoryPort, never()).save(any());
        verifyNoInteractions(auditPort, timeProviderPort, domainEventPublisherPort);
    }

    @Test
    void happyPath_closesClient_saves_audits_publishesEvent_andReturnsResult() {
        Client client = clientWithStatus(Status.SUSPENDED);

        when(clientRepositoryPort.findById(ClientId.of(CLIENT_ID_RAW))).thenReturn(Optional.of(client));
        when(ledgerQueryPort.netBalance(LedgerAccountRef.client(CLIENT_ID_RAW))).thenReturn(Money.zero());
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateClientStatusResult out = service.execute(cmd(adminActor(), Status.CLOSED.name(), REASON));

        assertEquals(Status.SUSPENDED.name(), out.previousStatus());
        assertEquals(Status.CLOSED.name(), out.newStatus());

        assertEquals(Status.CLOSED, client.status());
        verify(clientRepositoryPort).save(client);
        verify(auditPort).publish(any(AuditEvent.class));
        verify(domainEventPublisherPort).publish(any(ClientStatusChangedEvent.class));
    }

    @Test
    void throwsInvalidStatusTransition_whenTryingToSuspendClosedClient() {
        Client client = clientWithStatus(Status.CLOSED);

        when(clientRepositoryPort.findById(ClientId.of(CLIENT_ID_RAW))).thenReturn(Optional.of(client));

        assertThrows(InvalidStatusTransitionException.class, () ->
                service.execute(cmd(adminActor(), Status.SUSPENDED.name(), REASON))
        );

        verify(clientRepositoryPort, never()).save(any());
        verifyNoInteractions(auditPort, timeProviderPort, domainEventPublisherPort);
    }

    @Test
    void reasonDefaultsToNA_whenBlank() {
        Client client = clientWithStatus(Status.ACTIVE);

        when(clientRepositoryPort.findById(ClientId.of(CLIENT_ID_RAW))).thenReturn(Optional.of(client));
        when(timeProviderPort.now()).thenReturn(NOW);

        service.execute(cmd(adminActor(), Status.SUSPENDED.name(), "   "));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());

        assertEquals("N/A", auditCaptor.getValue().metadata().get("reason"));

        // event reason normalized as well
        ArgumentCaptor<ClientStatusChangedEvent> eventCaptor = ArgumentCaptor.forClass(ClientStatusChangedEvent.class);
        verify(domainEventPublisherPort).publish(eventCaptor.capture());
        assertEquals("N/A", eventCaptor.getValue().reason());
    }
}
