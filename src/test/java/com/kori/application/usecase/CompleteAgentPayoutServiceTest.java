package com.kori.application.usecase;

import com.kori.application.command.CompleteAgentPayoutCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.LedgerAppendPort;
import com.kori.application.port.out.PayoutRepositoryPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.ledger.LedgerEntryType;
import com.kori.domain.model.agent.AgentId;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.payout.Payout;
import com.kori.domain.model.payout.PayoutId;
import com.kori.domain.model.payout.PayoutStatus;
import com.kori.domain.model.transaction.TransactionId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class CompleteAgentPayoutServiceTest {

    @Mock TimeProviderPort timeProviderPort;
    @Mock PayoutRepositoryPort payoutRepositoryPort;
    @Mock LedgerAppendPort ledgerAppendPort;
    @Mock AuditPort auditPort;

    @InjectMocks CompleteAgentPayoutService service;

    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static final String ADMIN_ACTOR_ID = "admin-actor";
    private static final String NON_ADMIN_ACTOR_ID = "agent-actor";

    private static final UUID PAYOUT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final PayoutId PAYOUT_ID = new PayoutId(PAYOUT_UUID);

    private static final UUID AGENT_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final AgentId AGENT_ID = new AgentId(AGENT_UUID);

    private static final UUID TX_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final TransactionId TX_ID = new TransactionId(TX_UUID);

    private static final Money AMOUNT = Money.of(new BigDecimal("123.45"));

    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, ADMIN_ACTOR_ID, Map.of());
    }

    private static ActorContext nonAdminActor() {
        return new ActorContext(ActorType.AGENT, NON_ADMIN_ACTOR_ID, Map.of());
    }

    private static CompleteAgentPayoutCommand cmd(ActorContext actor) {
        return new CompleteAgentPayoutCommand(actor, PAYOUT_UUID.toString());
    }

    private static Payout requestedPayout() {
        return Payout.requested(PAYOUT_ID, AGENT_ID, TX_ID, AMOUNT, NOW.minusSeconds(120));
    }

    @Test
    void forbidden_whenActorIsNotAdmin() {
        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd(nonAdminActor())));
        verifyNoInteractions(payoutRepositoryPort, ledgerAppendPort, auditPort, timeProviderPort);
    }

    @Test
    void throwsNotFound_whenPayoutDoesNotExist() {
        when(payoutRepositoryPort.findById(PAYOUT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.execute(cmd(adminActor())));

        verify(payoutRepositoryPort).findById(PAYOUT_ID);
        verifyNoInteractions(ledgerAppendPort, auditPort, timeProviderPort);
    }

    @Test
    void forbidden_whenPayoutIsNotRequested() {
        Payout payout = requestedPayout();
        payout.fail(NOW.minusSeconds(10), "x"); // status = FAILED

        when(payoutRepositoryPort.findById(PAYOUT_ID)).thenReturn(Optional.of(payout));

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd(adminActor())));

        verifyNoInteractions(ledgerAppendPort, auditPort, timeProviderPort);
        verify(payoutRepositoryPort, never()).save(any(Payout.class));
    }

    @Test
    void happyPath_appendsLedger_completesPayout_persists_andAudits() {
        Payout payout = requestedPayout();
        when(payoutRepositoryPort.findById(PAYOUT_ID)).thenReturn(Optional.of(payout));
        when(timeProviderPort.now()).thenReturn(NOW);
        when(payoutRepositoryPort.save(any(Payout.class))).thenAnswer(inv -> inv.getArgument(0));

        service.execute(cmd(adminActor()));

        // ledger entries
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> ledgerCaptor = ArgumentCaptor.forClass(List.class);
        verify(ledgerAppendPort).append(ledgerCaptor.capture());
        List<LedgerEntry> entries = ledgerCaptor.getValue();

        assertEquals(2, entries.size());

        LedgerAccountRef agentAcc = LedgerAccountRef.agent(AGENT_UUID.toString());
        LedgerAccountRef clearingAcc = LedgerAccountRef.platformClearing();

        assertTrue(entries.stream().anyMatch(e ->
                e.transactionId().equals(TX_ID)
                        && e.type() == LedgerEntryType.DEBIT
                        && e.accountRef().equals(agentAcc)
                        && e.amount().equals(AMOUNT)
        ));

        assertTrue(entries.stream().anyMatch(e ->
                e.transactionId().equals(TX_ID)
                        && e.type() == LedgerEntryType.CREDIT
                        && e.accountRef().equals(clearingAcc)
                        && e.amount().equals(AMOUNT)
        ));

        // payout mutated & saved
        assertEquals(PayoutStatus.COMPLETED, payout.status());
        assertEquals(NOW, payout.completedAt());
        verify(payoutRepositoryPort).save(payout);

        // audit
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        AuditEvent event = auditCaptor.getValue();

        assertEquals("AGENT_PAYOUT_COMPLETED", event.action());
        assertEquals("ADMIN", event.actorType());
        assertEquals(ADMIN_ACTOR_ID, event.actorId());
        assertEquals(NOW, event.occurredAt());
        assertEquals(PAYOUT_UUID.toString(), event.metadata().get("payoutId"));
    }
}
