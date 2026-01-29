package com.kori.application.usecase;

import com.kori.application.command.UpdateAccountProfileStatusCommand;
import com.kori.application.events.AccountProfileStatusChangedEvent;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.out.AccountProfilePort;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.DomainEventPublisherPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateAccountProfileStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.common.InvalidStatusTransitionException;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerAccountType;
import com.kori.domain.model.account.AccountProfile;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class UpdateAccountProfileStatusServiceTest {

    // ======= mocks =======
    @Mock AccountProfilePort accountProfilePort;
    @Mock AuditPort auditPort;
    @Mock TimeProviderPort timeProviderPort;
    @Mock DomainEventPublisherPort domainEventPublisherPort;

    @InjectMocks UpdateAccountProfileStatusService service;

    // ======= constants =======
    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static final String ADMIN_ID = "admin-actor";
    private static final String MERCHANT_ID = "merchant-actor";

    private static final String ACCOUNT_TYPE_RAW = LedgerAccountType.MERCHANT.name();
    private static final String OWNER_REF = "M-123456";

    private static final LedgerAccountRef ACCOUNT_REF =
            new LedgerAccountRef(LedgerAccountType.MERCHANT, OWNER_REF);

    private static final String REASON = "Compliance";

    // ======= helpers =======
    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, ADMIN_ID, Map.of());
    }

    private static ActorContext nonAdminActor() {
        return new ActorContext(ActorType.MERCHANT, MERCHANT_ID, Map.of());
    }

    private static UpdateAccountProfileStatusCommand cmd(ActorContext actor, String targetStatus, String reason) {
        return new UpdateAccountProfileStatusCommand(actor, ACCOUNT_TYPE_RAW, OWNER_REF, targetStatus, reason);
    }

    private static AccountProfile profileWithStatus(Status status) {
        return new AccountProfile(ACCOUNT_REF, NOW.minusSeconds(120), status);
    }

    // ======= tests =======

    @Test
    void forbidden_whenActorIsNotAdmin() {
        assertThrows(ForbiddenOperationException.class, () ->
                service.execute(cmd(nonAdminActor(), Status.SUSPENDED.name(), REASON))
        );

        verifyNoInteractions(accountProfilePort, auditPort, timeProviderPort, domainEventPublisherPort);
    }

    @Test
    void throwsNotFound_whenAccountProfileDoesNotExist() {
        when(accountProfilePort.findByAccount(ACCOUNT_REF)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.execute(cmd(adminActor(), Status.SUSPENDED.name(), REASON))
        );

        verify(accountProfilePort).findByAccount(ACCOUNT_REF);
        verifyNoInteractions(auditPort, timeProviderPort, domainEventPublisherPort);
        verify(accountProfilePort, never()).save(any(AccountProfile.class));
    }

    @Test
    void happyPath_suspendsAccountProfile_saves_audits_publishesEvent_andReturnsResult() {
        AccountProfile profile = profileWithStatus(Status.ACTIVE);

        when(accountProfilePort.findByAccount(ACCOUNT_REF)).thenReturn(Optional.of(profile));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateAccountProfileStatusResult out = service.execute(cmd(adminActor(), Status.SUSPENDED.name(), REASON));

        assertEquals(ACCOUNT_TYPE_RAW, out.accountType());
        assertEquals(OWNER_REF, out.ownerRef());
        assertEquals(Status.ACTIVE.name(), out.previousStatus());
        assertEquals(Status.SUSPENDED.name(), out.newStatus());

        assertEquals(Status.SUSPENDED, profile.status());
        verify(accountProfilePort).save(profile);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        AuditEvent event = auditCaptor.getValue();

        assertEquals("ADMIN_UPDATE_ACCOUNT_PROFILE_STATUS_" + Status.SUSPENDED.name(), event.action());
        assertEquals(ActorType.ADMIN.name(), event.actorType());
        assertEquals(ADMIN_ID, event.actorId());
        assertEquals(NOW, event.occurredAt());

        assertEquals(ACCOUNT_TYPE_RAW, event.metadata().get("ledgerAccountType"));
        assertEquals(OWNER_REF, event.metadata().get("ownerRef"));
        assertEquals(Status.ACTIVE.name(), event.metadata().get("before"));
        assertEquals(Status.SUSPENDED.name(), event.metadata().get("after"));
        assertEquals(REASON, event.metadata().get("reason"));

        verify(domainEventPublisherPort).publish(any(AccountProfileStatusChangedEvent.class));
    }

    @Test
    void happyPath_activatesAccountProfile_saves_audits_publishesEvent_andReturnsResult() {
        AccountProfile profile = profileWithStatus(Status.SUSPENDED);

        when(accountProfilePort.findByAccount(ACCOUNT_REF)).thenReturn(Optional.of(profile));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateAccountProfileStatusResult out = service.execute(cmd(adminActor(), Status.ACTIVE.name(), REASON));

        assertEquals(Status.SUSPENDED.name(), out.previousStatus());
        assertEquals(Status.ACTIVE.name(), out.newStatus());

        assertEquals(Status.ACTIVE, profile.status());
        verify(accountProfilePort).save(profile);
        verify(auditPort).publish(any(AuditEvent.class));
        verify(domainEventPublisherPort).publish(any(AccountProfileStatusChangedEvent.class));
    }

    @Test
    void happyPath_closesAccountProfile_saves_audits_publishesEvent_andReturnsResult() {
        AccountProfile profile = profileWithStatus(Status.SUSPENDED);

        when(accountProfilePort.findByAccount(ACCOUNT_REF)).thenReturn(Optional.of(profile));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateAccountProfileStatusResult out = service.execute(cmd(adminActor(), Status.CLOSED.name(), REASON));

        assertEquals(Status.SUSPENDED.name(), out.previousStatus());
        assertEquals(Status.CLOSED.name(), out.newStatus());

        assertEquals(Status.CLOSED, profile.status());
        verify(accountProfilePort).save(profile);
        verify(auditPort).publish(any(AuditEvent.class));
        verify(domainEventPublisherPort).publish(any(AccountProfileStatusChangedEvent.class));
    }

    @Test
    void throwsInvalidStatusTransition_whenTryingToActivateClosedAccountProfile() {
        AccountProfile profile = profileWithStatus(Status.CLOSED);

        when(accountProfilePort.findByAccount(ACCOUNT_REF)).thenReturn(Optional.of(profile));

        assertThrows(InvalidStatusTransitionException.class, () ->
                service.execute(cmd(adminActor(), Status.ACTIVE.name(), REASON))
        );

        verify(accountProfilePort, never()).save(any());
        verifyNoInteractions(auditPort, timeProviderPort, domainEventPublisherPort);
    }

    @Test
    void reasonDefaultsToNA_whenBlank() {
        AccountProfile profile = profileWithStatus(Status.ACTIVE);

        when(accountProfilePort.findByAccount(ACCOUNT_REF)).thenReturn(Optional.of(profile));
        when(timeProviderPort.now()).thenReturn(NOW);

        service.execute(cmd(adminActor(), Status.SUSPENDED.name(), "   "));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());

        assertEquals("N/A", auditCaptor.getValue().metadata().get("reason"));

        ArgumentCaptor<AccountProfileStatusChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(AccountProfileStatusChangedEvent.class);
        verify(domainEventPublisherPort).publish(eventCaptor.capture());
        assertEquals("N/A", eventCaptor.getValue().reason());
    }
}
