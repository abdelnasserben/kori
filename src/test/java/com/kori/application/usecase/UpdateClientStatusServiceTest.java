package com.kori.application.usecase;

import com.kori.application.command.UpdateClientStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.ClientRepositoryPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateClientStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.common.InvalidStatusTransitionException;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.client.Client;
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
final class UpdateClientStatusServiceTest {

    // ======= mocks =======
    @Mock ClientRepositoryPort clientRepositoryPort;
    @Mock AuditPort auditPort;
    @Mock TimeProviderPort timeProviderPort;

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

        verifyNoInteractions(clientRepositoryPort, auditPort, timeProviderPort);
    }

    @Test
    void throwsNotFound_whenClientDoesNotExist() {
        when(clientRepositoryPort.findById(CLIENT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.execute(cmd(adminActor(), Status.SUSPENDED.name(), REASON))
        );

        verify(clientRepositoryPort).findById(CLIENT_ID);
        verifyNoInteractions(auditPort, timeProviderPort);
        verify(clientRepositoryPort, never()).save(any(Client.class));
    }

    @Test
    void happyPath_suspendsClient_saves_audits_andReturnsResult() {
        Client client = clientWithStatus(Status.ACTIVE);

        when(clientRepositoryPort.findById(CLIENT_ID)).thenReturn(Optional.of(client));
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
    }

    @Test
    void happyPath_activatesClient_saves_audits_andReturnsResult() {
        Client client = clientWithStatus(Status.SUSPENDED);

        when(clientRepositoryPort.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateClientStatusResult out = service.execute(cmd(adminActor(), Status.ACTIVE.name(), REASON));

        assertEquals(Status.SUSPENDED.name(), out.previousStatus());
        assertEquals(Status.ACTIVE.name(), out.newStatus());

        assertEquals(Status.ACTIVE, client.status());
        verify(clientRepositoryPort).save(client);
        verify(auditPort).publish(any(AuditEvent.class));
    }

    @Test
    void happyPath_closesClient_saves_audits_andReturnsResult() {
        Client client = clientWithStatus(Status.SUSPENDED);

        when(clientRepositoryPort.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateClientStatusResult out = service.execute(cmd(adminActor(), Status.CLOSED.name(), REASON));

        assertEquals(Status.SUSPENDED.name(), out.previousStatus());
        assertEquals(Status.CLOSED.name(), out.newStatus());

        assertEquals(Status.CLOSED, client.status());
        verify(clientRepositoryPort).save(client);
        verify(auditPort).publish(any(AuditEvent.class));
    }

    @Test
    void throwsInvalidStatusTransition_whenTryingToSuspendClosedClient() {
        Client client = clientWithStatus(Status.CLOSED);

        when(clientRepositoryPort.findById(CLIENT_ID)).thenReturn(Optional.of(client));

        assertThrows(InvalidStatusTransitionException.class, () ->
                service.execute(cmd(adminActor(), Status.SUSPENDED.name(), REASON))
        );

        verify(clientRepositoryPort, never()).save(any());
        verifyNoInteractions(auditPort, timeProviderPort);
    }

    @Test
    void reasonDefaultsToNA_whenBlank() {
        Client client = clientWithStatus(Status.ACTIVE);

        when(clientRepositoryPort.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(timeProviderPort.now()).thenReturn(NOW);

        service.execute(cmd(adminActor(), Status.SUSPENDED.name(), "   "));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());

        assertEquals("N/A", auditCaptor.getValue().metadata().get("reason"));
    }
}
