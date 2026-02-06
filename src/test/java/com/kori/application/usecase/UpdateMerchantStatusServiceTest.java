package com.kori.application.usecase;

import com.kori.application.command.UpdateMerchantStatusCommand;
import com.kori.application.events.MerchantStatusChangedEvent;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.out.*;
import com.kori.application.result.UpdateMerchantStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.common.InvalidStatusTransitionException;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.merchant.MerchantCode;
import com.kori.domain.model.merchant.MerchantId;
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
final class UpdateMerchantStatusServiceTest {

    // ======= mocks =======
    @Mock MerchantRepositoryPort merchantRepositoryPort;
    @Mock AuditPort auditPort;
    @Mock TimeProviderPort timeProviderPort;
    @Mock DomainEventPublisherPort domainEventPublisherPort;
    @Mock LedgerQueryPort ledgerQueryPort;

    @InjectMocks UpdateMerchantStatusService service;

    // ======= constants =======
    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static final String ADMIN_ID = "admin-actor";
    private static final String AGENT_ID = "agent-actor";

    private static final MerchantCode MERCHANT_CODE = MerchantCode.of("M-123456");

    private static final String REASON = "Ops action";

    // ======= helpers =======
    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, ADMIN_ID, Map.of());
    }

    private static ActorContext nonAdminActor() {
        return new ActorContext(ActorType.AGENT, AGENT_ID, Map.of());
    }

    private static UpdateMerchantStatusCommand cmd(ActorContext actor, String targetStatus, String reason) {
        return new UpdateMerchantStatusCommand(actor, MERCHANT_CODE.value(), targetStatus, reason);
    }

    private static Merchant merchantWithStatus(Status status) {
        return new Merchant(new MerchantId(UUID.randomUUID()), MERCHANT_CODE, status, NOW);
    }

    // ======= tests =======

    @Test
    void forbidden_whenActorIsNotAdmin() {
        assertThrows(ForbiddenOperationException.class, () ->
                service.execute(cmd(nonAdminActor(), Status.SUSPENDED.name(), REASON))
        );

        verifyNoInteractions(merchantRepositoryPort, auditPort, timeProviderPort, domainEventPublisherPort);
    }

    @Test
    void throwsNotFound_whenMerchantDoesNotExist() {
        when(merchantRepositoryPort.findByCode(MERCHANT_CODE)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.execute(cmd(adminActor(), Status.SUSPENDED.name(), REASON))
        );

        verify(merchantRepositoryPort).findByCode(MERCHANT_CODE);
        verifyNoInteractions(auditPort, timeProviderPort, domainEventPublisherPort);
        verify(merchantRepositoryPort, never()).save(any(Merchant.class));
    }

    @Test
    void happyPath_suspendsMerchant_saves_audits_publishesEvent_andReturnsResult() {
        Merchant merchant = merchantWithStatus(Status.ACTIVE);

        when(merchantRepositoryPort.findByCode(MERCHANT_CODE)).thenReturn(Optional.of(merchant));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateMerchantStatusResult out = service.execute(cmd(adminActor(), Status.SUSPENDED.name(), REASON));

        assertEquals(MERCHANT_CODE.value(), out.merchantCode());
        assertEquals(Status.ACTIVE.name(), out.previousStatus());
        assertEquals(Status.SUSPENDED.name(), out.newStatus());

        assertEquals(Status.SUSPENDED, merchant.status());
        verify(merchantRepositoryPort).save(merchant);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        AuditEvent event = auditCaptor.getValue();

        assertEquals("ADMIN_UPDATE_MERCHANT_STATUS_" + Status.SUSPENDED.name(), event.action());
        assertEquals(ActorType.ADMIN.name(), event.actorType());
        assertEquals(ADMIN_ID, event.actorId());
        assertEquals(NOW, event.occurredAt());

        assertEquals(MERCHANT_CODE.value(), event.metadata().get("merchantCode"));
        assertEquals(Status.ACTIVE.name(), event.metadata().get("before"));
        assertEquals(Status.SUSPENDED.name(), event.metadata().get("after"));
        assertEquals(REASON, event.metadata().get("reason"));

        verify(domainEventPublisherPort).publish(any(MerchantStatusChangedEvent.class));
    }

    @Test
    void happyPath_activatesMerchant_saves_audits_publishesEvent_andReturnsResult() {
        Merchant merchant = merchantWithStatus(Status.SUSPENDED);

        when(merchantRepositoryPort.findByCode(MERCHANT_CODE)).thenReturn(Optional.of(merchant));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateMerchantStatusResult out = service.execute(cmd(adminActor(), Status.ACTIVE.name(), REASON));

        assertEquals(Status.SUSPENDED.name(), out.previousStatus());
        assertEquals(Status.ACTIVE.name(), out.newStatus());

        assertEquals(Status.ACTIVE, merchant.status());
        verify(merchantRepositoryPort).save(merchant);
        verify(auditPort).publish(any(AuditEvent.class));
        verify(domainEventPublisherPort).publish(any(MerchantStatusChangedEvent.class));
    }

    @Test
    void happyPath_closesMerchant_saves_audits_publishesEvent_andReturnsResult() {
        Merchant merchant = merchantWithStatus(Status.SUSPENDED);

        when(merchantRepositoryPort.findByCode(MERCHANT_CODE)).thenReturn(Optional.of(merchant));
        when(timeProviderPort.now()).thenReturn(NOW);
        when(ledgerQueryPort.netBalance(any(LedgerAccountRef.class))).thenReturn(Money.zero());

        UpdateMerchantStatusResult out = service.execute(cmd(adminActor(), Status.CLOSED.name(), REASON));

        assertEquals(Status.SUSPENDED.name(), out.previousStatus());
        assertEquals(Status.CLOSED.name(), out.newStatus());

        assertEquals(Status.CLOSED, merchant.status());
        verify(merchantRepositoryPort).save(merchant);
        verify(auditPort).publish(any(AuditEvent.class));
        verify(domainEventPublisherPort).publish(any(MerchantStatusChangedEvent.class));
    }

    @Test
    void throwsInvalidStatusTransition_whenTryingToSuspendClosedMerchant() {
        Merchant merchant = merchantWithStatus(Status.CLOSED);

        when(merchantRepositoryPort.findByCode(MERCHANT_CODE)).thenReturn(Optional.of(merchant));

        assertThrows(InvalidStatusTransitionException.class, () ->
                service.execute(cmd(adminActor(), Status.SUSPENDED.name(), REASON))
        );

        verify(merchantRepositoryPort, never()).save(any());
        verifyNoInteractions(auditPort, timeProviderPort, domainEventPublisherPort);
    }

    @Test
    void reasonDefaultsToNA_whenBlank() {
        Merchant merchant = merchantWithStatus(Status.ACTIVE);

        when(merchantRepositoryPort.findByCode(MERCHANT_CODE)).thenReturn(Optional.of(merchant));
        when(timeProviderPort.now()).thenReturn(NOW);

        service.execute(cmd(adminActor(), Status.SUSPENDED.name(), "   "));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        assertEquals("N/A", auditCaptor.getValue().metadata().get("reason"));

        ArgumentCaptor<MerchantStatusChangedEvent> eventCaptor = ArgumentCaptor.forClass(MerchantStatusChangedEvent.class);
        verify(domainEventPublisherPort).publish(eventCaptor.capture());
        assertEquals("N/A", eventCaptor.getValue().reason());
    }
}
