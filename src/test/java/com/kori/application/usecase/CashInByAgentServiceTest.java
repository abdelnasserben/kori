package com.kori.application.usecase;

import com.kori.application.command.CashInByAgentCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.IdempotencyConflictException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.OperationStatusGuards;
import com.kori.application.port.out.*;
import com.kori.application.result.CashInByAgentResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.ledger.LedgerEntryType;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.agent.AgentId;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.common.Status;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class CashInByAgentServiceTest {

    @Mock TimeProviderPort timeProviderPort;
    @Mock IdempotencyPort idempotencyPort;
    @Mock IdGeneratorPort idGeneratorPort;
    @Mock AgentRepositoryPort agentRepositoryPort;
    @Mock ClientRepositoryPort clientRepositoryPort;
    @Mock CardRepositoryPort cardRepositoryPort;
    @Mock LedgerQueryPort ledgerQueryPort;
    @Mock TransactionRepositoryPort transactionRepositoryPort;
    @Mock LedgerAppendPort ledgerAppendPort;
    @Mock AuditPort auditPort;
    @Mock OperationStatusGuards operationStatusGuards;

    @InjectMocks CashInByAgentService service;

    private static final String IDEM_KEY = "idem-1";
    private static final String REQUEST_HASH = "request-hash";
    private static final String ACTOR_ID = "11111111-1111-1111-1111-111111111111";
    private static final UUID AGENT_UUID = UUID.fromString(ACTOR_ID);
    private static final UUID CLIENT_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TX_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final String PHONE = "+2690000000";
    private static final BigDecimal AMOUNT_BD = new BigDecimal("100.00");
    private static final Money AMOUNT = Money.positive(AMOUNT_BD);

    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static ActorContext agentActor() {
        return new ActorContext(ActorType.AGENT, ACTOR_ID, Map.of());
    }

    private static CashInByAgentCommand cmd() {
        return new CashInByAgentCommand(
                IDEM_KEY,
                REQUEST_HASH,
                agentActor(),
                PHONE,
                AMOUNT_BD
        );
    }

    private static Agent activeAgent() {
        return new Agent(new AgentId(AGENT_UUID), AgentCode.of("A-123456"), NOW.minusSeconds(60), Status.ACTIVE);
    }

    private static Client activeClient() {
        return new Client(new ClientId(CLIENT_UUID), PHONE, Status.ACTIVE, NOW.minusSeconds(120));
    }

    @Test
    void returnsCachedResult_whenIdempotencyKeyAlreadyProcessed() {
        CashInByAgentResult cached = new CashInByAgentResult(
                "tx-1",
                ACTOR_ID,
                CLIENT_UUID.toString(),
                PHONE,
                AMOUNT_BD
        );

        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, CashInByAgentResult.class))
                .thenReturn(Optional.of(cached));

        CashInByAgentResult out = service.execute(cmd());

        assertSame(cached, out);
        verify(idempotencyPort).find(IDEM_KEY, REQUEST_HASH, CashInByAgentResult.class);

        verifyNoMoreInteractions(
                timeProviderPort,
                idGeneratorPort,
                agentRepositoryPort,
                clientRepositoryPort,
                cardRepositoryPort,
                ledgerQueryPort,
                transactionRepositoryPort,
                ledgerAppendPort,
                auditPort,
                operationStatusGuards,
                idempotencyPort
        );
    }

    @Test
    void success_cash_in_by_agent() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, CashInByAgentResult.class))
                .thenReturn(Optional.empty());

        Agent agent = activeAgent();
        Client client = activeClient();

        when(agentRepositoryPort.findById(new AgentId(AGENT_UUID))).thenReturn(Optional.of(agent));
        doNothing().when(operationStatusGuards).requireActiveAgent(agent);

        when(clientRepositoryPort.findByPhoneNumber(PHONE)).thenReturn(Optional.of(client));
        doNothing().when(operationStatusGuards).requireActiveClient(client);

        when(timeProviderPort.now()).thenReturn(NOW);
        when(idGeneratorPort.newUuid()).thenReturn(TX_UUID);
        when(transactionRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CashInByAgentResult result = service.execute(cmd());

        assertEquals(TX_UUID.toString(), result.transactionId());
        assertEquals(AGENT_UUID.toString(), result.agentId());
        assertEquals(CLIENT_UUID.toString(), result.clientId());
        assertEquals(PHONE, result.clientPhoneNumber());
        assertEquals(AMOUNT_BD, result.amount());

        ArgumentCaptor<List<LedgerEntry>> entriesCaptor = ArgumentCaptor.forClass(List.class);
        verify(ledgerAppendPort).append(entriesCaptor.capture());
        List<LedgerEntry> entries = entriesCaptor.getValue();
        assertEquals(2, entries.size());
        assertTrue(entries.stream().anyMatch(e ->
                e.accountRef().equals(LedgerAccountRef.platformClearing())
                        && e.type() == LedgerEntryType.DEBIT
                        && e.amount().equals(AMOUNT)
        ));
        assertTrue(entries.stream().anyMatch(e ->
                e.accountRef().equals(LedgerAccountRef.client(CLIENT_UUID.toString()))
                        && e.type() == LedgerEntryType.CREDIT
                        && e.amount().equals(AMOUNT)
        ));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        assertEquals("AGENT_CASH_IN", auditCaptor.getValue().action());

        verify(idempotencyPort).save(eq(IDEM_KEY), eq(REQUEST_HASH), any(CashInByAgentResult.class));
    }

    @Test
    void forbidden_when_agent_inactive() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, CashInByAgentResult.class))
                .thenReturn(Optional.empty());

        Agent agent = activeAgent();
        when(agentRepositoryPort.findById(new AgentId(AGENT_UUID))).thenReturn(Optional.of(agent));
        doThrow(new ForbiddenOperationException("AGENT_NOT_ACTIVE"))
                .when(operationStatusGuards).requireActiveAgent(agent);

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd()));
        verify(operationStatusGuards).requireActiveAgent(agent);
        verifyNoInteractions(clientRepositoryPort, ledgerAppendPort, auditPort);
    }

    @Test
    void notFound_when_client_missing() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, CashInByAgentResult.class))
                .thenReturn(Optional.empty());

        Agent agent = activeAgent();
        when(agentRepositoryPort.findById(new AgentId(AGENT_UUID))).thenReturn(Optional.of(agent));
        doNothing().when(operationStatusGuards).requireActiveAgent(agent);

        when(clientRepositoryPort.findByPhoneNumber(PHONE)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.execute(cmd()));
        verify(clientRepositoryPort).findByPhoneNumber(PHONE);
        verifyNoInteractions(ledgerAppendPort, auditPort);
    }

    @Test
    void conflict_when_idempotency_key_reused_with_different_payload() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, CashInByAgentResult.class))
                .thenThrow(new IdempotencyConflictException("Conflict"));

        assertThrows(IdempotencyConflictException.class, () -> service.execute(cmd()));
        verify(idempotencyPort).find(IDEM_KEY, REQUEST_HASH, CashInByAgentResult.class);
    }
}
