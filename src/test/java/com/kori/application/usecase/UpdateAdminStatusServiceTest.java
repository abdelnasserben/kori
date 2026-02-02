package com.kori.application.usecase;

import com.kori.application.command.UpdateAdminStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.out.AdminRepositoryPort;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateAdminStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.common.InvalidStatusTransitionException;
import com.kori.domain.model.admin.Admin;
import com.kori.domain.model.admin.AdminId;
import com.kori.domain.model.audit.AuditEvent;
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
final class UpdateAdminStatusServiceTest {

    @Mock AdminRepositoryPort adminRepositoryPort;
    @Mock AuditPort auditPort;
    @Mock TimeProviderPort timeProviderPort;

    @InjectMocks UpdateAdminStatusService service;

    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static final String ADMIN_ID = "admin-actor";
    private static final String ADMIN_TARGET_ID = "55555555-5555-5555-5555-555555555555";

    private static final String REASON = "Ops action";

    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, ADMIN_ID, Map.of());
    }

    private static ActorContext nonAdminActor() {
        return new ActorContext(ActorType.AGENT, "agent-actor", Map.of());
    }

    private static UpdateAdminStatusCommand cmd(ActorContext actor, String targetStatus, String reason) {
        return new UpdateAdminStatusCommand(actor, ADMIN_TARGET_ID, targetStatus, reason);
    }

    private static Admin adminWithStatus(Status status) {
        return new Admin(new AdminId(UUID.fromString(ADMIN_TARGET_ID)), status, NOW);
    }

    @Test
    void forbidden_whenActorIsNotAdmin() {
        assertThrows(ForbiddenOperationException.class, () ->
                service.execute(cmd(nonAdminActor(), Status.SUSPENDED.name(), REASON))
        );

        verifyNoInteractions(adminRepositoryPort, auditPort, timeProviderPort);
    }

    @Test
    void throwsNotFound_whenAdminDoesNotExist() {
        when(adminRepositoryPort.findById(new AdminId(UUID.fromString(ADMIN_TARGET_ID)))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.execute(cmd(adminActor(), Status.SUSPENDED.name(), REASON))
        );

        verify(adminRepositoryPort).findById(new AdminId(UUID.fromString(ADMIN_TARGET_ID)));
        verifyNoInteractions(auditPort, timeProviderPort);
        verify(adminRepositoryPort, org.mockito.Mockito.never()).save(any(Admin.class));
    }

    @Test
    void happyPath_suspendsAdmin_saves_audits_andReturnsResult() {
        Admin admin = adminWithStatus(Status.ACTIVE);

        when(adminRepositoryPort.findById(new AdminId(UUID.fromString(ADMIN_TARGET_ID)))).thenReturn(Optional.of(admin));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateAdminStatusResult out = service.execute(cmd(adminActor(), Status.SUSPENDED.name(), REASON));

        assertEquals(ADMIN_TARGET_ID, out.adminId());
        assertEquals(Status.ACTIVE.name(), out.previousStatus());
        assertEquals(Status.SUSPENDED.name(), out.newStatus());

        assertEquals(Status.SUSPENDED, admin.status());
        verify(adminRepositoryPort).save(admin);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        AuditEvent event = auditCaptor.getValue();

        assertEquals("ADMIN_UPDATE_ADMIN_STATUS_" + Status.SUSPENDED.name(), event.action());
        assertEquals(ActorType.ADMIN.name(), event.actorType());
        assertEquals(ADMIN_ID, event.actorId());
        assertEquals(NOW, event.occurredAt());
        assertEquals(ADMIN_TARGET_ID, event.metadata().get("adminId"));
        assertEquals(Status.ACTIVE.name(), event.metadata().get("before"));
        assertEquals(Status.SUSPENDED.name(), event.metadata().get("after"));
        assertEquals(REASON, event.metadata().get("reason"));
    }

    @Test
    void happyPath_activatesAdmin_saves_audits_andReturnsResult() {
        Admin admin = adminWithStatus(Status.SUSPENDED);

        when(adminRepositoryPort.findById(new AdminId(UUID.fromString(ADMIN_TARGET_ID)))).thenReturn(Optional.of(admin));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateAdminStatusResult out = service.execute(cmd(adminActor(), Status.ACTIVE.name(), REASON));

        assertEquals(Status.SUSPENDED.name(), out.previousStatus());
        assertEquals(Status.ACTIVE.name(), out.newStatus());

        assertEquals(Status.ACTIVE, admin.status());
        verify(adminRepositoryPort).save(admin);
        verify(auditPort).publish(any(AuditEvent.class));
    }

    @Test
    void happyPath_closesAdmin_saves_audits_andReturnsResult() {
        Admin admin = adminWithStatus(Status.SUSPENDED);

        when(adminRepositoryPort.findById(new AdminId(UUID.fromString(ADMIN_TARGET_ID)))).thenReturn(Optional.of(admin));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateAdminStatusResult out = service.execute(cmd(adminActor(), Status.CLOSED.name(), REASON));

        assertEquals(Status.SUSPENDED.name(), out.previousStatus());
        assertEquals(Status.CLOSED.name(), out.newStatus());

        assertEquals(Status.CLOSED, admin.status());
        verify(adminRepositoryPort).save(admin);
        verify(auditPort).publish(any(AuditEvent.class));
    }

    @Test
    void throwsInvalidStatusTransition_whenTryingToSuspendClosedAdmin() {
        Admin admin = adminWithStatus(Status.CLOSED);

        when(adminRepositoryPort.findById(new AdminId(UUID.fromString(ADMIN_TARGET_ID)))).thenReturn(Optional.of(admin));

        assertThrows(InvalidStatusTransitionException.class, () ->
                service.execute(cmd(adminActor(), Status.SUSPENDED.name(), REASON))
        );

        verify(adminRepositoryPort, org.mockito.Mockito.never()).save(any());
        verifyNoInteractions(auditPort, timeProviderPort);
    }

    @Test
    void reasonDefaultsToNA_whenBlank() {
        Admin admin = adminWithStatus(Status.ACTIVE);

        when(adminRepositoryPort.findById(new AdminId(UUID.fromString(ADMIN_TARGET_ID)))).thenReturn(Optional.of(admin));
        when(timeProviderPort.now()).thenReturn(NOW);

        service.execute(cmd(adminActor(), Status.SUSPENDED.name(), "   "));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        assertEquals("N/A", auditCaptor.getValue().metadata().get("reason"));
    }
}
