package com.kori.application.usecase;

import com.kori.application.command.AdminUpdateAccountStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.out.AccountRepositoryPort;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.IdempotencyPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.AdminUpdateAccountStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.account.Account;
import com.kori.domain.model.account.AccountId;
import com.kori.domain.model.account.AdminAccountStatusAction;
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
class AdminUpdateAccountStatusServiceTest {

    @Mock TimeProviderPort timeProviderPort;
    @Mock IdempotencyPort idempotencyPort;
    @Mock AccountRepositoryPort accountRepositoryPort;
    @Mock AuditPort auditPort;

    private AdminUpdateAccountStatusService service;

    @BeforeEach
    void setUp() {
        service = new AdminUpdateAccountStatusService(
                timeProviderPort, idempotencyPort, accountRepositoryPort, auditPort
        );
    }

    @Test
    void returnsCachedResultWhenIdempotencyHit() {
        AdminUpdateAccountStatusResult cached = new AdminUpdateAccountStatusResult("acc-1", "ACTIVE");
        when(idempotencyPort.find("idem-1", AdminUpdateAccountStatusResult.class)).thenReturn(Optional.of(cached));

        AdminUpdateAccountStatusCommand cmd = new AdminUpdateAccountStatusCommand(
                "idem-1",
                new ActorContext(ActorType.ADMIN, "admin-actor", Map.of()),
                "acc-1",
                AdminAccountStatusAction.ACTIVE,
                "reason"
        );

        AdminUpdateAccountStatusResult result = service.execute(cmd);

        assertSame(cached, result);
        verifyNoInteractions(accountRepositoryPort, auditPort);
    }

    @Test
    void happyPath_setsSuspended_andAudits_andSavesIdempotency() {
        Instant now = Instant.parse("2026-01-21T10:15:30Z");
        when(timeProviderPort.now()).thenReturn(now);
        when(idempotencyPort.find("idem-2", AdminUpdateAccountStatusResult.class)).thenReturn(Optional.empty());

        Account existing = new Account(AccountId.of("acc-1"), ClientId.of("c-1"), Status.ACTIVE);
        when(accountRepositoryPort.findById(AccountId.of("acc-1"))).thenReturn(Optional.of(existing));
        when(accountRepositoryPort.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUpdateAccountStatusCommand cmd = new AdminUpdateAccountStatusCommand(
                "idem-2",
                new ActorContext(ActorType.ADMIN, "admin-actor", Map.of()),
                "acc-1",
                AdminAccountStatusAction.SUSPENDED,
                "risk"
        );

        AdminUpdateAccountStatusResult result = service.execute(cmd);

        assertEquals("acc-1", result.accountId());
        assertEquals("SUSPENDED", result.status());

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepositoryPort).save(accountCaptor.capture());
        assertEquals(Status.SUSPENDED, accountCaptor.getValue().status());

        verify(auditPort).publish(argThat(ev ->
                ev.action().equals("ADMIN_SET_ACCOUNT_STATUS_SUSPENDED")
                        && ev.actorType().equals("ADMIN")
                        && ev.actorId().equals("admin-actor")
                        && ev.occurredAt().equals(now)
                        && "acc-1".equals(ev.metadata().get("accountId"))
        ));

        verify(idempotencyPort).save("idem-2", result);
    }

    @Test
    void forbidden_whenActorNotAdmin() {
        when(idempotencyPort.find("idem-3", AdminUpdateAccountStatusResult.class)).thenReturn(Optional.empty());

        AdminUpdateAccountStatusCommand cmd = new AdminUpdateAccountStatusCommand(
                "idem-3",
                new ActorContext(ActorType.AGENT, "agent-actor", Map.of()),
                "acc-1",
                AdminAccountStatusAction.INACTIVE,
                "reason"
        );

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd));
    }
}
