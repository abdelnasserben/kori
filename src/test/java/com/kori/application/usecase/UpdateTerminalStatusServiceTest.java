package com.kori.application.usecase;

import com.kori.application.command.UpdateTerminalStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.TerminalRepositoryPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateTerminalStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.common.InvalidStatusTransitionException;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.MerchantId;
import com.kori.domain.model.terminal.Terminal;
import com.kori.domain.model.terminal.TerminalId;
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
final class UpdateTerminalStatusServiceTest {

    // ======= mocks =======
    @Mock TerminalRepositoryPort terminalRepositoryPort;
    @Mock AuditPort auditPort;
    @Mock TimeProviderPort timeProviderPort;

    @InjectMocks UpdateTerminalStatusService service;

    // ======= constants =======
    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static final String ADMIN_ID = "admin-actor";
    private static final String AGENT_ID = "agent-actor";

    private static final TerminalId TERMINAL_ID = new TerminalId(UUID.randomUUID());
    private static final String TERMINAL_ID_RAW = TERMINAL_ID.value().toString();

    private static final String REASON = "Ops action";

    // ======= helpers =======
    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, ADMIN_ID, Map.of());
    }

    private static ActorContext nonAdminActor() {
        return new ActorContext(ActorType.AGENT, AGENT_ID, Map.of());
    }

    private static UpdateTerminalStatusCommand cmd(ActorContext actor, String targetStatus, String reason) {
        return new UpdateTerminalStatusCommand(actor, TERMINAL_ID_RAW, targetStatus, reason);
    }

    private static Terminal terminalWithStatus(Status status) {
        return new Terminal(TERMINAL_ID, new MerchantId(UUID.randomUUID()), status, NOW);
    }

    // ======= tests =======

    @Test
    void forbidden_whenActorIsNotAdmin() {
        assertThrows(ForbiddenOperationException.class, () ->
                service.execute(cmd(nonAdminActor(), Status.SUSPENDED.name(), REASON))
        );

        verifyNoInteractions(terminalRepositoryPort, auditPort, timeProviderPort);
    }

    @Test
    void throwsNotFound_whenClientDoesNotExist() {
        when(terminalRepositoryPort.findById(TERMINAL_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.execute(cmd(adminActor(), Status.SUSPENDED.name(), REASON))
        );

        verify(terminalRepositoryPort).findById(TERMINAL_ID);
        verifyNoInteractions(auditPort, timeProviderPort);
        verify(terminalRepositoryPort, never()).save(any(Terminal.class));
    }

    @Test
    void happyPath_suspendsClient_saves_audits_andReturnsResult() {
        Terminal terminal = terminalWithStatus(Status.ACTIVE);

        when(terminalRepositoryPort.findById(TERMINAL_ID)).thenReturn(Optional.of(terminal));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateTerminalStatusResult out = service.execute(cmd(adminActor(), Status.SUSPENDED.name(), REASON));

        assertEquals(TERMINAL_ID_RAW, out.terminalId());
        assertEquals(Status.ACTIVE.name(), out.previousStatus());
        assertEquals(Status.SUSPENDED.name(), out.newStatus());

        assertEquals(Status.SUSPENDED, terminal.status());
        verify(terminalRepositoryPort).save(terminal);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        AuditEvent event = auditCaptor.getValue();

        assertEquals("ADMIN_UPDATE_TERMINAL_STATUS_" + Status.SUSPENDED.name(), event.action());
        assertEquals(ActorType.ADMIN.name(), event.actorType());
        assertEquals(ADMIN_ID, event.actorId());
        assertEquals(NOW, event.occurredAt());

        assertEquals(TERMINAL_ID_RAW, event.metadata().get("terminalId"));
        assertEquals(Status.ACTIVE.name(), event.metadata().get("before"));
        assertEquals(Status.SUSPENDED.name(), event.metadata().get("after"));
        assertEquals(REASON, event.metadata().get("reason"));
    }

    @Test
    void happyPath_activatesClient_saves_audits_andReturnsResult() {
        Terminal terminal = terminalWithStatus(Status.SUSPENDED);

        when(terminalRepositoryPort.findById(TERMINAL_ID)).thenReturn(Optional.of(terminal));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateTerminalStatusResult out = service.execute(cmd(adminActor(), Status.ACTIVE.name(), REASON));

        assertEquals(Status.SUSPENDED.name(), out.previousStatus());
        assertEquals(Status.ACTIVE.name(), out.newStatus());

        assertEquals(Status.ACTIVE, terminal.status());
        verify(terminalRepositoryPort).save(terminal);
        verify(auditPort).publish(any(AuditEvent.class));
    }

    @Test
    void happyPath_closesClient_saves_audits_andReturnsResult() {
        Terminal terminal = terminalWithStatus(Status.SUSPENDED);

        when(terminalRepositoryPort.findById(TERMINAL_ID)).thenReturn(Optional.of(terminal));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateTerminalStatusResult out = service.execute(cmd(adminActor(), Status.CLOSED.name(), REASON));

        assertEquals(Status.SUSPENDED.name(), out.previousStatus());
        assertEquals(Status.CLOSED.name(), out.newStatus());

        assertEquals(Status.CLOSED, terminal.status());
        verify(terminalRepositoryPort).save(terminal);
        verify(auditPort).publish(any(AuditEvent.class));
    }

    @Test
    void throwsInvalidStatusTransition_whenTryingToSuspendClosedClient() {
        Terminal terminal = terminalWithStatus(Status.CLOSED);

        when(terminalRepositoryPort.findById(TERMINAL_ID)).thenReturn(Optional.of(terminal));

        assertThrows(InvalidStatusTransitionException.class, () ->
                service.execute(cmd(adminActor(), Status.SUSPENDED.name(), REASON))
        );

        verify(terminalRepositoryPort, never()).save(any());
        verifyNoInteractions(auditPort, timeProviderPort);
    }

    @Test
    void reasonDefaultsToNA_whenBlank() {
        Terminal terminal = terminalWithStatus(Status.ACTIVE);

        when(terminalRepositoryPort.findById(TERMINAL_ID)).thenReturn(Optional.of(terminal));
        when(timeProviderPort.now()).thenReturn(NOW);

        service.execute(cmd(adminActor(), Status.SUSPENDED.name(), "   "));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());

        assertEquals("N/A", auditCaptor.getValue().metadata().get("reason"));
    }
}
