package com.kori.application.usecase;

import com.kori.application.command.UpdateAgentStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.out.AgentRepositoryPort;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateAgentStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.common.InvalidStatusTransitionException;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.agent.AgentId;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class UpdateAgentStatusServiceTest {

    // ======= mocks =======
    @Mock AgentRepositoryPort agentRepositoryPort;
    @Mock AuditPort auditPort;
    @Mock TimeProviderPort timeProviderPort;

    @InjectMocks UpdateAgentStatusService service;

    // ======= constants =======
    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static final String ADMIN_ID = "admin-actor";
    private static final String AGENT_ID = "agent-actor";

    private static final AgentCode AGENT_CODE = AgentCode.of("A-123456");

    private static final String REASON = "Ops action";

    // ======= helpers =======
    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, ADMIN_ID, Map.of());
    }

    private static ActorContext nonAdminActor() {
        return new ActorContext(ActorType.AGENT, AGENT_ID, Map.of());
    }

    private static UpdateAgentStatusCommand cmd(ActorContext actor, String targetStatus, String reason) {
        return new UpdateAgentStatusCommand(actor, AGENT_CODE.value(), targetStatus, reason);
    }

    private static Agent agentWithStatus(Status status) {
        return new Agent(new AgentId(UUID.randomUUID()), AGENT_CODE, NOW, status);
    }

    // ======= tests =======

    @Test
    void forbidden_whenActorIsNotAdmin() {
        assertThrows(ForbiddenOperationException.class, () ->
                service.execute(cmd(nonAdminActor(), Status.SUSPENDED.name(), REASON))
        );

        verifyNoInteractions(agentRepositoryPort, auditPort, timeProviderPort);
    }

    @Test
    void throwsNotFound_whenMerchantDoesNotExist() {
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.execute(cmd(adminActor(), Status.SUSPENDED.name(), REASON))
        );

        verify(agentRepositoryPort).findByCode(AGENT_CODE);
        verifyNoInteractions(auditPort, timeProviderPort);
        verify(agentRepositoryPort, never()).save(any(Agent.class));
    }

    @Test
    void happyPath_suspendsClient_saves_audits_andReturnsResult() {
        Agent agent = agentWithStatus(Status.ACTIVE);

        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(agent));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateAgentStatusResult out = service.execute(cmd(adminActor(), Status.SUSPENDED.name(), REASON));

        assertEquals(AGENT_CODE.value(), out.agentCode());
        assertEquals(Status.ACTIVE.name(), out.previousStatus());
        assertEquals(Status.SUSPENDED.name(), out.newStatus());

        assertEquals(Status.SUSPENDED, agent.status());
        verify(agentRepositoryPort).save(agent);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        AuditEvent event = auditCaptor.getValue();

        assertEquals("ADMIN_UPDATE_AGENT_STATUS_" + Status.SUSPENDED.name(), event.action());
        assertEquals(ActorType.ADMIN.name(), event.actorType());
        assertEquals(ADMIN_ID, event.actorId());
        assertEquals(NOW, event.occurredAt());

        assertEquals(AGENT_CODE.value(), event.metadata().get("agentCode"));
        assertEquals(Status.ACTIVE.name(), event.metadata().get("before"));
        assertEquals(Status.SUSPENDED.name(), event.metadata().get("after"));
        assertEquals(REASON, event.metadata().get("reason"));
    }

    @Test
    void happyPath_activatesClient_saves_audits_andReturnsResult() {
        Agent agent = agentWithStatus(Status.SUSPENDED);

        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(agent));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateAgentStatusResult out = service.execute(cmd(adminActor(), Status.ACTIVE.name(), REASON));

        assertEquals(Status.SUSPENDED.name(), out.previousStatus());
        assertEquals(Status.ACTIVE.name(), out.newStatus());

        assertEquals(Status.ACTIVE, agent.status());
        verify(agentRepositoryPort).save(agent);
        verify(auditPort).publish(any(AuditEvent.class));
    }

    @Test
    void happyPath_closesClient_saves_audits_andReturnsResult() {
        Agent agent = agentWithStatus(Status.SUSPENDED);

        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(agent));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateAgentStatusResult out = service.execute(cmd(adminActor(), Status.CLOSED.name(), REASON));

        assertEquals(Status.SUSPENDED.name(), out.previousStatus());
        assertEquals(Status.CLOSED.name(), out.newStatus());

        assertEquals(Status.CLOSED, agent.status());
        verify(agentRepositoryPort).save(agent);
        verify(auditPort).publish(any(AuditEvent.class));
    }

    @Test
    void throwsInvalidStatusTransition_whenTryingToSuspendClosedClient() {
        Agent agent = agentWithStatus(Status.CLOSED);

        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(agent));

        assertThrows(InvalidStatusTransitionException.class, () ->
                service.execute(cmd(adminActor(), Status.SUSPENDED.name(), REASON))
        );

        verify(agentRepositoryPort, never()).save(any());
        verifyNoInteractions(auditPort, timeProviderPort);
    }

    @Test
    void reasonDefaultsToNA_whenBlank() {
        Agent agent = agentWithStatus(Status.ACTIVE);

        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(agent));
        when(timeProviderPort.now()).thenReturn(NOW);

        service.execute(cmd(adminActor(), Status.SUSPENDED.name(), "   "));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());

        assertEquals("N/A", auditCaptor.getValue().metadata().get("reason"));
    }
}
