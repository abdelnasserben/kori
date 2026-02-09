package com.kori.application.usecase;

import com.kori.application.command.CreateAdminCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.out.*;
import com.kori.application.result.CreateAdminResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.admin.Admin;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.common.Status;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class CreateAdminServiceTest {

    @Mock AdminRepositoryPort adminRepositoryPort;
    @Mock IdempotencyPort idempotencyPort;
    @Mock TimeProviderPort timeProviderPort;
    @Mock IdGeneratorPort idGeneratorPort;
    @Mock AuditPort auditPort;

    @InjectMocks CreateAdminService service;

    private static final String IDEM_KEY = "idem-1";
    private static final String REQUEST_HASH = "request-hash";
    private static final String ADMIN_ID = "admin-actor";
    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static final UUID ADMIN_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, ADMIN_ID, Map.of());
    }

    private static ActorContext nonAdminActor() {
        return new ActorContext(ActorType.AGENT, "agent-actor", Map.of());
    }

    private static CreateAdminCommand cmd(ActorContext actor) {
        return new CreateAdminCommand(IDEM_KEY, REQUEST_HASH, actor);
    }

    @BeforeEach
    void setUp() {
        lenient().when(idempotencyPort.reserve(anyString(), anyString(), any())).thenReturn(true);
    }

    @Test
    void forbidden_whenActorIsNotAdmin() {
        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd(nonAdminActor())));

        verifyNoInteractions(idempotencyPort, adminRepositoryPort, timeProviderPort, idGeneratorPort, auditPort);
    }

    @Test
    void returnsCachedResult_whenIdempotencyKeyAlreadyProcessed() {
        CreateAdminResult cached = new CreateAdminResult("admin-1");
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, CreateAdminResult.class)).thenReturn(Optional.of(cached));

        CreateAdminResult out = service.execute(cmd(adminActor()));

        assertSame(cached, out);
        verify(idempotencyPort).find(IDEM_KEY, REQUEST_HASH, CreateAdminResult.class);

        verifyNoMoreInteractions(idGeneratorPort, timeProviderPort, adminRepositoryPort, auditPort, idempotencyPort);
    }

    @Test
    void happyPath_createsAdmin_audits_andSavesIdempotency() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, CreateAdminResult.class)).thenReturn(Optional.empty());
        when(idGeneratorPort.newUuid()).thenReturn(ADMIN_UUID);
        when(timeProviderPort.now()).thenReturn(NOW);

        CreateAdminResult out = service.execute(cmd(adminActor()));

        assertEquals(ADMIN_UUID.toString(), out.adminId());

        ArgumentCaptor<Admin> adminCaptor = ArgumentCaptor.forClass(Admin.class);
        verify(adminRepositoryPort).save(adminCaptor.capture());
        Admin savedAdmin = adminCaptor.getValue();

        assertEquals(ADMIN_UUID, savedAdmin.id().value());
        assertEquals(Status.ACTIVE, savedAdmin.status());
        assertEquals(NOW, savedAdmin.createdAt());

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        AuditEvent event = auditCaptor.getValue();

        assertEquals("ADMIN_CREATED", event.action());
        assertEquals("ADMIN", event.actorType());
        assertEquals(ADMIN_ID, event.actorId());
        assertEquals(NOW, event.occurredAt());
        assertEquals(ADMIN_ID, event.metadata().get("adminId"));
        assertEquals(ADMIN_UUID.toString(), event.metadata().get("createdAdminId"));

        verify(idempotencyPort).save(eq(IDEM_KEY), eq(REQUEST_HASH), any(CreateAdminResult.class));
    }
}
