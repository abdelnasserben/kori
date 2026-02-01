package com.kori.application.usecase;

import com.kori.application.command.RequestAgentPayoutCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.out.*;
import com.kori.application.result.AgentPayoutResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.agent.AgentId;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.payout.Payout;
import com.kori.domain.model.payout.PayoutId;
import com.kori.domain.model.payout.PayoutStatus;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class RequestAgentPayoutServiceTest {

    // ======= mocks =======
    @Mock TimeProviderPort timeProviderPort;
    @Mock IdempotencyPort idempotencyPort;

    @Mock AgentRepositoryPort agentRepositoryPort;
    @Mock LedgerQueryPort ledgerQueryPort;
    @Mock TransactionRepositoryPort transactionRepositoryPort;
    @Mock PayoutRepositoryPort payoutRepositoryPort;
    @Mock AuditPort auditPort;
    @Mock IdGeneratorPort idGeneratorPort;

    @InjectMocks RequestAgentPayoutService service;

    // ======= constants =======
    private static final String IDEM_KEY = "idem-1";
    private static final String ADMIN_ACTOR_ID = "admin-actor";
    private static final String NON_ADMIN_ACTOR_ID = "agent-actor";

    private static final String AGENT_CODE_RAW = "A-123456";
    private static final AgentCode AGENT_CODE = AgentCode.of(AGENT_CODE_RAW);

    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static final UUID AGENT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final AgentId AGENT_ID = new AgentId(AGENT_UUID);

    private static final UUID TX_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final TransactionId TX_ID = new TransactionId(TX_UUID);

    private static final UUID PAYOUT_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final PayoutId PAYOUT_ID = new PayoutId(PAYOUT_UUID);

    private static final Money DUE = Money.of(new BigDecimal("123.45"));

    // ======= helpers =======
    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, ADMIN_ACTOR_ID, Map.of());
    }

    private static ActorContext nonAdminActor() {
        return new ActorContext(ActorType.AGENT, NON_ADMIN_ACTOR_ID, Map.of());
    }

    private static RequestAgentPayoutCommand cmd(ActorContext actor) {
        return new RequestAgentPayoutCommand(IDEM_KEY, actor, AGENT_CODE_RAW);
    }

    private static Agent activeAgent() {
        return new Agent(AGENT_ID, AGENT_CODE, NOW.minusSeconds(60), Status.ACTIVE);
    }

    @Test
    void returnsCachedResult_whenIdempotencyKeyAlreadyProcessed() {
        AgentPayoutResult cached = new AgentPayoutResult(
                "tx-1",
                "payout-1",
                AGENT_CODE_RAW,
                new BigDecimal("10.00"),
                PayoutStatus.REQUESTED.name()
        );

        when(idempotencyPort.find(IDEM_KEY, AgentPayoutResult.class)).thenReturn(Optional.of(cached));

        AgentPayoutResult out = service.execute(cmd(adminActor()));

        assertSame(cached, out);
        verify(idempotencyPort).find(IDEM_KEY, AgentPayoutResult.class);

        verifyNoMoreInteractions(
                timeProviderPort,
                agentRepositoryPort,
                ledgerQueryPort,
                transactionRepositoryPort,
                payoutRepositoryPort,
                auditPort,
                idGeneratorPort,
                idempotencyPort
        );
    }

    @Test
    void forbidden_whenActorIsNotAdmin() {
        when(idempotencyPort.find(IDEM_KEY, AgentPayoutResult.class)).thenReturn(Optional.empty());

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd(nonAdminActor())));

        verify(idempotencyPort).find(IDEM_KEY, AgentPayoutResult.class);
        verifyNoMoreInteractions(idempotencyPort);

        verifyNoInteractions(
                timeProviderPort,
                agentRepositoryPort,
                ledgerQueryPort,
                transactionRepositoryPort,
                payoutRepositoryPort,
                auditPort,
                idGeneratorPort
        );
    }

    @Test
    void forbidden_whenAgentNotFound() {
        when(idempotencyPort.find(IDEM_KEY, AgentPayoutResult.class)).thenReturn(Optional.empty());
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.execute(cmd(adminActor())));

        verify(agentRepositoryPort).findByCode(AGENT_CODE);
        verifyNoInteractions(ledgerQueryPort, payoutRepositoryPort, transactionRepositoryPort, auditPort, idGeneratorPort);
    }

    @Test
    void forbidden_whenAgentNotActive() {
        when(idempotencyPort.find(IDEM_KEY, AgentPayoutResult.class)).thenReturn(Optional.empty());

        Agent inactive = new Agent(AGENT_ID, AGENT_CODE, NOW.minusSeconds(60), Status.SUSPENDED);
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(inactive));

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd(adminActor())));

        verifyNoInteractions(ledgerQueryPort, payoutRepositoryPort, transactionRepositoryPort, auditPort, idGeneratorPort);
    }

    @Test
    void forbidden_whenPayoutAlreadyRequestedForAgent() {
        when(idempotencyPort.find(IDEM_KEY, AgentPayoutResult.class)).thenReturn(Optional.empty());
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(activeAgent()));
        when(payoutRepositoryPort.existsRequestedForAgent(AGENT_ID)).thenReturn(true);

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd(adminActor())));

        verify(payoutRepositoryPort).existsRequestedForAgent(AGENT_ID);
        verifyNoInteractions(ledgerQueryPort, transactionRepositoryPort, auditPort, idGeneratorPort);
    }

    @Test
    void forbidden_whenNoPayoutDue() {
        when(idempotencyPort.find(IDEM_KEY, AgentPayoutResult.class)).thenReturn(Optional.empty());
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(activeAgent()));
        when(payoutRepositoryPort.existsRequestedForAgent(AGENT_ID)).thenReturn(false);

        LedgerAccountRef agentAcc = LedgerAccountRef.agent(AGENT_UUID.toString());
        when(ledgerQueryPort.netBalance(agentAcc)).thenReturn(Money.zero());

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd(adminActor())));

        verify(ledgerQueryPort).netBalance(agentAcc);
        verifyNoInteractions(transactionRepositoryPort, auditPort, idGeneratorPort);
    }

    @Test
    void happyPath_createsTransactionAndRequestedPayout_audits_andSavesIdempotency() {
        when(idempotencyPort.find(IDEM_KEY, AgentPayoutResult.class)).thenReturn(Optional.empty());
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(activeAgent()));
        when(payoutRepositoryPort.existsRequestedForAgent(AGENT_ID)).thenReturn(false);

        LedgerAccountRef agentAcc = LedgerAccountRef.agent(AGENT_UUID.toString());
        when(ledgerQueryPort.netBalance(agentAcc)).thenReturn(DUE);

        when(timeProviderPort.now()).thenReturn(NOW);

        when(idGeneratorPort.newUuid()).thenReturn(TX_UUID, PAYOUT_UUID);
        when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payoutRepositoryPort.save(any(Payout.class))).thenAnswer(inv -> inv.getArgument(0));

        AgentPayoutResult out = service.execute(cmd(adminActor()));

        assertEquals(TX_UUID.toString(), out.transactionId());
        assertEquals(PAYOUT_UUID.toString(), out.payoutId());
        assertEquals(AGENT_CODE_RAW, out.agentCode());
        assertEquals(DUE.asBigDecimal(), out.amount());
        assertEquals(PayoutStatus.REQUESTED.name(), out.payoutStatus());

        // Transaction saved
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort).save(txCaptor.capture());
        Transaction savedTx = txCaptor.getValue();
        assertEquals(TX_ID, savedTx.id());
        assertEquals(DUE, savedTx.amount());

        // Payout saved
        ArgumentCaptor<Payout> payoutCaptor = ArgumentCaptor.forClass(Payout.class);
        verify(payoutRepositoryPort).save(payoutCaptor.capture());
        Payout savedPayout = payoutCaptor.getValue();
        assertEquals(PAYOUT_ID, savedPayout.id());
        assertEquals(AGENT_ID, savedPayout.agentId());
        assertEquals(TX_ID, savedPayout.transactionId());
        assertEquals(DUE, savedPayout.amount());
        assertEquals(PayoutStatus.REQUESTED, savedPayout.status());

        // Audit
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        AuditEvent event = auditCaptor.getValue();

        assertEquals("AGENT_PAYOUT_REQUESTED", event.action());
        assertEquals("ADMIN", event.actorType());
        assertEquals(ADMIN_ACTOR_ID, event.actorId());
        assertEquals(NOW, event.occurredAt());
        assertEquals(TX_UUID.toString(), event.metadata().get("transactionId"));
        assertEquals(AGENT_CODE_RAW, event.metadata().get("agentCode"));
        assertEquals(PAYOUT_UUID.toString(), event.metadata().get("payoutId"));

        verify(idempotencyPort).save(eq(IDEM_KEY), any(AgentPayoutResult.class));
    }
}
