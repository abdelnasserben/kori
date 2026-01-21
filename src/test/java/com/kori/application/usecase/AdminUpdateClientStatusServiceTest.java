package com.kori.application.usecase;

import com.kori.application.command.AdminUpdateClientStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.ClientRepositoryPort;
import com.kori.application.port.out.IdempotencyPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.AdminUpdateClientStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.client.AdminClientStatusAction;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Status;
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
class AdminUpdateClientStatusServiceTest {

    @Mock TimeProviderPort timeProviderPort;
    @Mock IdempotencyPort idempotencyPort;
    @Mock ClientRepositoryPort clientRepositoryPort;
    @Mock AuditPort auditPort;

    private AdminUpdateClientStatusService service;

    @BeforeEach
    void setUp() {
        service = new AdminUpdateClientStatusService(
                timeProviderPort, idempotencyPort, clientRepositoryPort, auditPort
        );
    }

    @Test
    void returnsCachedResultWhenIdempotencyHit() {
        AdminUpdateClientStatusResult cached = new AdminUpdateClientStatusResult("c-1", "ACTIVE");
        when(idempotencyPort.find("idem-1", AdminUpdateClientStatusResult.class)).thenReturn(Optional.of(cached));

        AdminUpdateClientStatusCommand cmd = new AdminUpdateClientStatusCommand(
                "idem-1",
                new ActorContext(ActorType.ADMIN, "admin-actor", Map.of()),
                "c-1",
                AdminClientStatusAction.ACTIVE,
                "reason"
        );

        AdminUpdateClientStatusResult result = service.execute(cmd);

        assertSame(cached, result);
        verifyNoInteractions(clientRepositoryPort, auditPort);
    }

    @Test
    void happyPath_setsClosed_andAudits_andSavesIdempotency() {
        Instant now = Instant.parse("2026-01-21T10:15:30Z");
        when(timeProviderPort.now()).thenReturn(now);
        when(idempotencyPort.find("idem-2", AdminUpdateClientStatusResult.class)).thenReturn(Optional.empty());

        Client existing = new Client(ClientId.of("c-1"), "+2690000000", Status.ACTIVE);
        when(clientRepositoryPort.findById(ClientId.of("c-1"))).thenReturn(Optional.of(existing));
        when(clientRepositoryPort.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUpdateClientStatusCommand cmd = new AdminUpdateClientStatusCommand(
                "idem-2",
                new ActorContext(ActorType.ADMIN, "admin-actor", Map.of()),
                "c-1",
                AdminClientStatusAction.CLOSED,
                "fraud"
        );

        AdminUpdateClientStatusResult result = service.execute(cmd);

        assertEquals("c-1", result.clientId());
        assertEquals("CLOSED", result.status());

        ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepositoryPort).save(clientCaptor.capture());
        assertEquals(Status.CLOSED, clientCaptor.getValue().status());

        verify(auditPort).publish(argThat(ev ->
                ev.action().equals("ADMIN_SET_CLIENT_STATUS_CLOSED")
                        && ev.actorType().equals("ADMIN")
                        && ev.actorId().equals("admin-actor")
                        && ev.occurredAt().equals(now)
                        && "c-1".equals(ev.metadata().get("clientId"))
        ));

        verify(idempotencyPort).save("idem-2", result);
    }

    @Test
    void forbidden_whenActorNotAdmin() {
        when(idempotencyPort.find("idem-3", AdminUpdateClientStatusResult.class)).thenReturn(Optional.empty());

        AdminUpdateClientStatusCommand cmd = new AdminUpdateClientStatusCommand(
                "idem-3",
                new ActorContext(ActorType.AGENT, "agent-actor", Map.of()),
                "c-1",
                AdminClientStatusAction.SUSPENDED,
                "reason"
        );

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd));
    }
}
