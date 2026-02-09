package com.kori.application.usecase;

import com.kori.application.command.AgentBankDepositReceiptCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.idempotency.IdempotencyClaim;
import com.kori.application.port.out.*;
import com.kori.application.result.AgentBankDepositReceiptResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.agent.AgentId;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionType;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentBankDepositReceiptServiceTest {

    @Mock TimeProviderPort timeProviderPort;
    @Mock
    IdempotencyPort idempotencyPort;
    @Mock IdGeneratorPort idGeneratorPort;
    @Mock AgentRepositoryPort agentRepositoryPort;
    @Mock TransactionRepositoryPort transactionRepositoryPort;
    @Mock LedgerAppendPort ledgerAppendPort;
    @Mock AuditPort auditPort;

    @InjectMocks AgentBankDepositReceiptService service;

    private static final String IDEM_KEY = "idem-1";
    private static final String REQUEST_HASH = "request-hash";
    private static final String AGENT_CODE_RAW = "A-778899";
    private static final UUID AGENT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TX_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    @Test
    void returnsCachedResult_whenIdempotencyKeyAlreadyProcessed() {
        var cached = new AgentBankDepositReceiptResult("tx-1", AGENT_CODE_RAW, new BigDecimal("10.00"));
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, AgentBankDepositReceiptResult.class)).thenReturn(IdempotencyClaim.completed(cached));

        var out = service.execute(command(adminActor(), new BigDecimal("10.00")));

        assertSame(cached, out);
        verifyNoInteractions(timeProviderPort, idGeneratorPort, agentRepositoryPort, transactionRepositoryPort, ledgerAppendPort, auditPort);
    }

    @Test
    void forbidden_whenActorIsNotAdmin() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, AgentBankDepositReceiptResult.class)).thenReturn(IdempotencyClaim.claimed());

        assertThrows(ForbiddenOperationException.class,
                () -> service.execute(command(new ActorContext(ActorType.AGENT, "agent-1", Map.of()), new BigDecimal("10.00"))));

        verifyNoInteractions(timeProviderPort, idGeneratorPort, agentRepositoryPort, transactionRepositoryPort, ledgerAppendPort, auditPort);
    }

    @Test
    void notFound_whenAgentDoesNotExist() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, AgentBankDepositReceiptResult.class)).thenReturn(IdempotencyClaim.claimed());
        when(agentRepositoryPort.findByCode(AgentCode.of(AGENT_CODE_RAW))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.execute(command(adminActor(), new BigDecimal("10.00"))));

        verifyNoInteractions(timeProviderPort, idGeneratorPort, transactionRepositoryPort, ledgerAppendPort, auditPort);
    }

    @Test
    void happyPath_recordsLedgerAuditAndIdempotency() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, AgentBankDepositReceiptResult.class)).thenReturn(IdempotencyClaim.claimed());
        when(agentRepositoryPort.findByCode(AgentCode.of(AGENT_CODE_RAW))).thenReturn(Optional.of(activeAgent()));
        when(idGeneratorPort.newUuid()).thenReturn(TX_UUID);
        when(timeProviderPort.now()).thenReturn(NOW);
        when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        var out = service.execute(command(adminActor(), new BigDecimal("120.50")));

        assertEquals(TX_UUID.toString(), out.transactionId());
        assertEquals(AGENT_CODE_RAW, out.agentCode());
        assertEquals(new BigDecimal("120.50"), out.amount());

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort).save(txCaptor.capture());
        assertEquals(TransactionType.AGENT_BANK_DEPOSIT_RECEIPT, txCaptor.getValue().type());

        var bankAcc = LedgerAccountRef.platformBank();
        var clearingAcc = LedgerAccountRef.agentCashClearing(AGENT_UUID.toString());
        verify(ledgerAppendPort).append(argThat(entries -> entries.size() == 2
                && entries.stream().anyMatch(e -> e.accountRef().equals(bankAcc) && e.type().name().equals("DEBIT"))
                && entries.stream().anyMatch(e -> e.accountRef().equals(clearingAcc) && e.type().name().equals("CREDIT"))));

        verify(auditPort).publish(any());
        verify(idempotencyPort).complete(eq(IDEM_KEY), eq(REQUEST_HASH), any(AgentBankDepositReceiptResult.class));
    }

    private AgentBankDepositReceiptCommand command(ActorContext actor, BigDecimal amount) {
        return new AgentBankDepositReceiptCommand(IDEM_KEY, REQUEST_HASH, actor, AGENT_CODE_RAW, amount);
    }

    private ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, "admin-1", Map.of());
    }

    private Agent activeAgent() {
        return new Agent(new AgentId(AGENT_UUID), AgentCode.of(AGENT_CODE_RAW), NOW.minusSeconds(60), Status.ACTIVE);
    }
}
