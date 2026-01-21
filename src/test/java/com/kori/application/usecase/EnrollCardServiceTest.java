package com.kori.application.usecase;

import com.kori.application.command.EnrollCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.out.*;
import com.kori.application.result.EnrollCardResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.account.Account;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollCardServiceTest {

    @Mock TimeProviderPort timeProviderPort;
    @Mock IdempotencyPort idempotencyPort;
    @Mock ClientRepositoryPort clientRepositoryPort;
    @Mock AccountRepositoryPort accountRepositoryPort;
    @Mock CardRepositoryPort cardRepositoryPort;
    @Mock AgentRepositoryPort agentRepositoryPort;
    @Mock TransactionRepositoryPort transactionRepositoryPort;
    @Mock FeePolicyPort feePolicyPort;
    @Mock CommissionPolicyPort commissionPolicyPort;
    @Mock LedgerAppendPort ledgerAppendPort;
    @Mock AuditPort auditPort;

    private EnrollCardService service;

    @BeforeEach
    void setUp() {
        service = new EnrollCardService(
                timeProviderPort, idempotencyPort,
                clientRepositoryPort, accountRepositoryPort, cardRepositoryPort,
                agentRepositoryPort, transactionRepositoryPort,
                feePolicyPort, commissionPolicyPort,
                ledgerAppendPort, auditPort
        );
    }

    @Test
    void returnsCachedResultWhenIdempotencyHit() {
        EnrollCardResult cached = new EnrollCardResult(
                "tx1", "c1", "a1", "card1",
                BigDecimal.valueOf(100), BigDecimal.valueOf(10),
                false, false
        );

        when(idempotencyPort.find("idem-1", EnrollCardResult.class)).thenReturn(Optional.of(cached));

        EnrollCardCommand cmd = new EnrollCardCommand(
                "idem-1",
                new ActorContext(ActorType.AGENT, "agent-actor", Map.of()),
                "261000000000",
                "CARD-UID-1",
                "1234",
                "agent-1"
        );

        EnrollCardResult result = service.execute(cmd);

        assertSame(cached, result);
        verifyNoInteractions(clientRepositoryPort, accountRepositoryPort, cardRepositoryPort,
                agentRepositoryPort, transactionRepositoryPort, feePolicyPort, commissionPolicyPort,
                ledgerAppendPort, auditPort);
    }

    @Test
    void enrollCard_createsClientAndAccount_whenAbsent_appendsLedger_andAudits() {
        Instant now = Instant.parse("2026-01-21T10:15:30Z");
        when(timeProviderPort.now()).thenReturn(now);

        when(idempotencyPort.find("idem-2", EnrollCardResult.class)).thenReturn(Optional.empty());
        when(agentRepositoryPort.existsById("agent-1")).thenReturn(true);
        when(cardRepositoryPort.findByCardUid("CARD-UID-1")).thenReturn(Optional.empty());

        when(clientRepositoryPort.findByPhoneNumber("261000000000")).thenReturn(Optional.empty());
        when(clientRepositoryPort.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        when(accountRepositoryPort.findByClientId(any(ClientId.class))).thenReturn(Optional.empty());
        when(accountRepositoryPort.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        when(cardRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Money cardPrice = Money.of(BigDecimal.valueOf(100));
        Money commission = Money.of(BigDecimal.valueOf(10));
        when(feePolicyPort.cardEnrollmentPrice()).thenReturn(cardPrice);
        when(commissionPolicyPort.cardEnrollmentAgentCommission()).thenReturn(commission);

        when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollCardCommand cmd = new EnrollCardCommand(
                "idem-2",
                new ActorContext(ActorType.AGENT, "agent-actor", Map.of()),
                "261000000000",
                "CARD-UID-1",
                "1234",
                "agent-1"
        );

        EnrollCardResult result = service.execute(cmd);

        assertNotNull(result.transactionId());
        assertNotNull(result.clientId());
        assertNotNull(result.accountId());
        assertNotNull(result.cardId());
        assertEquals(BigDecimal.valueOf(100).setScale(2), result.cardPrice());
        assertEquals(BigDecimal.valueOf(10).setScale(2), result.agentCommission());
        assertTrue(result.clientCreated());
        assertTrue(result.accountCreated());

        ArgumentCaptor<List<LedgerEntry>> ledgerCaptor = ArgumentCaptor.forClass(List.class);
        verify(ledgerAppendPort).append(ledgerCaptor.capture());
        List<LedgerEntry> entries = ledgerCaptor.getValue();
        assertEquals(2, entries.size());

        // audit
        verify(auditPort).publish(argThat(ev ->
                ev.action().equals("ENROLL_CARD")
                        && ev.actorType().equals("AGENT")
                        && ev.actorId().equals("agent-actor")
                        && ev.occurredAt().equals(now)
                        && "agent-1".equals(ev.metadata().get("agentId"))
        ));

        verify(idempotencyPort).save("idem-2", result);
    }

    @Test
    void enrollCard_forbidden_whenActorNotAgent() {
        when(idempotencyPort.find("idem-3", EnrollCardResult.class)).thenReturn(Optional.empty());

        EnrollCardCommand cmd = new EnrollCardCommand(
                "idem-3",
                new ActorContext(ActorType.ADMIN, "admin-1", Map.of()),
                "261000000000",
                "CARD-UID-1",
                "1234",
                "agent-1"
        );

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd));
    }
}
