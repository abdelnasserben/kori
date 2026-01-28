package com.kori.application.usecase;

import com.kori.application.command.CreateAgentCommand;
import com.kori.application.exception.ApplicationException;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.out.*;
import com.kori.application.result.CreateAgentResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class CreateAgentServiceTest {

    // ======= mocks =======
    @Mock AgentRepositoryPort agentRepositoryPort;
    @Mock AccountProfilePort accountProfilePort;
    @Mock IdempotencyPort idempotencyPort;
    @Mock AuditPort auditPort;
    @Mock TimeProviderPort timeProviderPort;
    @Mock CodeGeneratorPort codeGeneratorPort;
    @Mock IdGeneratorPort idGeneratorPort;

    @InjectMocks CreateAgentService service;

    // ======= constants =======
    private static final String IDEM_KEY = "idem-1";
    private static final String ADMIN_ID = "admin-actor";
    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static final UUID AGENT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String DIGITS_1 = "123456";
    private static final String DIGITS_2 = "654321";
    private static final String AGENT_CODE_1 = "A-123456";
    private static final String AGENT_CODE_2 = "A-654321";

    // ======= helpers =======
    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, ADMIN_ID, Map.of());
    }

    private static ActorContext nonAdminActor() {
        return new ActorContext(ActorType.AGENT, "agent-actor", Map.of());
    }

    private static CreateAgentCommand cmd(ActorContext actor) {
        return new CreateAgentCommand(IDEM_KEY, actor);
    }

    @Test
    void forbidden_whenActorIsNotAdmin() {
        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd(nonAdminActor())));

        // actor check happens before idempotency
        verifyNoInteractions(idempotencyPort, agentRepositoryPort, accountProfilePort, auditPort, timeProviderPort, codeGeneratorPort, idGeneratorPort);
    }

    @Test
    void returnsCachedResult_whenIdempotencyKeyAlreadyProcessed() {
        CreateAgentResult cached = new CreateAgentResult("agent-1", "A-000001");
        when(idempotencyPort.find(IDEM_KEY, CreateAgentResult.class)).thenReturn(Optional.of(cached));

        CreateAgentResult out = service.execute(cmd(adminActor()));

        assertSame(cached, out);
        verify(idempotencyPort).find(IDEM_KEY, CreateAgentResult.class);

        verifyNoMoreInteractions(
                idGeneratorPort,
                codeGeneratorPort,
                timeProviderPort,
                agentRepositoryPort,
                accountProfilePort,
                auditPort,
                idempotencyPort
        );
    }

    @Test
    void happyPath_createsAgentAndAccountProfile_audits_andSavesIdempotency() {
        when(idempotencyPort.find(IDEM_KEY, CreateAgentResult.class)).thenReturn(Optional.empty());

        when(codeGeneratorPort.next6Digits()).thenReturn(DIGITS_1);
        when(agentRepositoryPort.existsByCode(AgentCode.of(AGENT_CODE_1))).thenReturn(false);

        when(idGeneratorPort.newUuid()).thenReturn(AGENT_UUID);
        when(timeProviderPort.now()).thenReturn(NOW);

        LedgerAccountRef agentAcc = LedgerAccountRef.agent(AGENT_UUID.toString());
        when(accountProfilePort.findByAccount(agentAcc)).thenReturn(Optional.empty());

        CreateAgentResult out = service.execute(cmd(adminActor()));

        assertEquals(AGENT_UUID.toString(), out.agentId());
        assertEquals(AGENT_CODE_1, out.agentCode());

        // Agent saved
        ArgumentCaptor<Agent> agentCaptor = ArgumentCaptor.forClass(Agent.class);
        verify(agentRepositoryPort).save(agentCaptor.capture());
        Agent savedAgent = agentCaptor.getValue();

        assertEquals(AGENT_UUID, savedAgent.id().value());
        assertEquals(AgentCode.of(AGENT_CODE_1), savedAgent.code());
        assertEquals(Status.ACTIVE, savedAgent.status());
        assertEquals(NOW, savedAgent.createdAt());

        // Profile saved
        ArgumentCaptor<AccountProfile> profileCaptor = ArgumentCaptor.forClass(AccountProfile.class);
        verify(accountProfilePort).save(profileCaptor.capture());
        AccountProfile savedProfile = profileCaptor.getValue();

        assertEquals(agentAcc, savedProfile.account());
        assertEquals(Status.ACTIVE, savedProfile.status());
        assertEquals(NOW, savedProfile.createdAt());

        // Audit
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        AuditEvent event = auditCaptor.getValue();

        assertEquals("AGENT_CREATED", event.action());
        assertEquals("ADMIN", event.actorType());
        assertEquals(ADMIN_ID, event.actorId());
        assertEquals(NOW, event.occurredAt());
        assertEquals(ADMIN_ID, event.metadata().get("adminId"));
        assertEquals(AGENT_CODE_1, event.metadata().get("agentCode"));

        verify(idempotencyPort).save(eq(IDEM_KEY), any(CreateAgentResult.class));
    }

    @Test
    void codeGeneration_retriesOnCollision_thenSucceeds() {
        when(idempotencyPort.find(IDEM_KEY, CreateAgentResult.class)).thenReturn(Optional.empty());

        when(codeGeneratorPort.next6Digits()).thenReturn(DIGITS_1, DIGITS_2);
        when(agentRepositoryPort.existsByCode(AgentCode.of(AGENT_CODE_1))).thenReturn(true);
        when(agentRepositoryPort.existsByCode(AgentCode.of(AGENT_CODE_2))).thenReturn(false);

        when(idGeneratorPort.newUuid()).thenReturn(AGENT_UUID);
        when(timeProviderPort.now()).thenReturn(NOW);

        LedgerAccountRef agentAcc = LedgerAccountRef.agent(AGENT_UUID.toString());
        when(accountProfilePort.findByAccount(agentAcc)).thenReturn(Optional.empty());

        CreateAgentResult out = service.execute(cmd(adminActor()));

        assertEquals(AGENT_CODE_2, out.agentCode());
        verify(codeGeneratorPort, times(2)).next6Digits();
    }

    @Test
    void throwsApplicationException_whenCannotGenerateUniqueCode_afterMaxAttempts() {
        when(idempotencyPort.find(IDEM_KEY, CreateAgentResult.class)).thenReturn(Optional.empty());

        when(codeGeneratorPort.next6Digits()).thenReturn("000001");
        when(agentRepositoryPort.existsByCode(any(AgentCode.class))).thenReturn(true);

        assertThrows(ApplicationException.class, () -> service.execute(cmd(adminActor())));

        verify(codeGeneratorPort, times(20)).next6Digits();
        verify(agentRepositoryPort, times(20)).existsByCode(any(AgentCode.class));

        verifyNoInteractions(idGeneratorPort, timeProviderPort, accountProfilePort, auditPort);
        verify(idempotencyPort, never()).save(anyString(), any());
    }

    @Test
    void forbidden_whenAccountProfileAlreadyExists() {
        when(idempotencyPort.find(IDEM_KEY, CreateAgentResult.class)).thenReturn(Optional.empty());

        when(codeGeneratorPort.next6Digits()).thenReturn(DIGITS_1);
        when(agentRepositoryPort.existsByCode(AgentCode.of(AGENT_CODE_1))).thenReturn(false);

        when(idGeneratorPort.newUuid()).thenReturn(AGENT_UUID);
        when(timeProviderPort.now()).thenReturn(NOW);

        LedgerAccountRef agentAcc = LedgerAccountRef.agent(AGENT_UUID.toString());
        when(accountProfilePort.findByAccount(agentAcc)).thenReturn(Optional.of(mock(AccountProfile.class)));

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd(adminActor())));

        // Agent is saved BEFORE profile duplication check (as per service code)
        verify(agentRepositoryPort).save(any(Agent.class));

        verify(accountProfilePort, never()).save(any(AccountProfile.class));
        verify(idempotencyPort, never()).save(anyString(), any());
        verifyNoInteractions(auditPort);
    }
}
