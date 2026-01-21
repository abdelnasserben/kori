package com.kori.application.usecase;

import com.kori.application.command.AgentPayoutCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.out.*;
import com.kori.application.result.AgentPayoutResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.payout.Payout;
import com.kori.domain.model.transaction.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentPayoutServiceTest {

    @Mock TimeProviderPort timeProviderPort;
    @Mock IdempotencyPort idempotencyPort;
    @Mock AgentRepositoryPort agentRepositoryPort;
    @Mock LedgerQueryPort ledgerQueryPort;
    @Mock LedgerAppendPort ledgerAppendPort;
    @Mock TransactionRepositoryPort transactionRepositoryPort;
    @Mock PayoutRepositoryPort payoutRepositoryPort;
    @Mock AuditPort auditPort;

    private AgentPayoutService service;

    @BeforeEach
    void setUp() {
        service = new AgentPayoutService(
                timeProviderPort, idempotencyPort,
                agentRepositoryPort, ledgerQueryPort,
                ledgerAppendPort, transactionRepositoryPort,
                payoutRepositoryPort,
                auditPort
        );
    }

    @Test
    void payout_happyPath() {
        Instant now = Instant.parse("2026-01-21T10:15:30Z");
        when(timeProviderPort.now()).thenReturn(now);
        when(idempotencyPort.find("idem-1", AgentPayoutResult.class)).thenReturn(Optional.empty());
        when(agentRepositoryPort.existsById("agent-1")).thenReturn(true);
        when(ledgerQueryPort.agentAvailableBalance("agent-1"))
                .thenReturn(Money.of(BigDecimal.valueOf(100)));

        when(payoutRepositoryPort.save(any(Payout.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(transactionRepositoryPort.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AgentPayoutCommand cmd = new AgentPayoutCommand(
                "idem-1",
                new ActorContext(ActorType.ADMIN, "admin-actor", Map.of()),
                "agent-1"
        );

        AgentPayoutResult result = service.execute(cmd);

        // Spec: must pay exactly what is due (available balance)
        assertEquals(BigDecimal.valueOf(100).setScale(2), result.amount());
        assertEquals("agent-1", result.agentId());
        verify(ledgerAppendPort).append(any());
        verify(auditPort).publish(any());
        verify(payoutRepositoryPort, times(2)).save(any());
        verify(idempotencyPort).save("idem-1", result);
    }

    @Test
    void payout_forbidden_whenNoPayoutDue() {
        when(idempotencyPort.find("idem-2", AgentPayoutResult.class)).thenReturn(Optional.empty());
        when(agentRepositoryPort.existsById("agent-1")).thenReturn(true);
        when(ledgerQueryPort.agentAvailableBalance("agent-1"))
                .thenReturn(Money.zero());

        AgentPayoutCommand cmd = new AgentPayoutCommand(
                "idem-2",
                new ActorContext(ActorType.ADMIN, "admin-actor", Map.of()),
                "agent-1"
        );

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd));
    }
}
