package com.kori.application.usecase;

import com.kori.application.command.ReversalCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.out.*;
import com.kori.application.result.ReversalResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccount;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.ledger.LedgerEntryType;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;
import com.kori.domain.model.transaction.TransactionType;
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
class ReversalServiceTest {

    @Mock TimeProviderPort timeProviderPort;
    @Mock IdempotencyPort idempotencyPort;
    @Mock TransactionRepositoryPort transactionRepositoryPort;
    @Mock LedgerQueryPort ledgerQueryPort;
    @Mock LedgerAppendPort ledgerAppendPort;
    @Mock AuditPort auditPort;

    private ReversalService service;

    @BeforeEach
    void setUp() {
        service = new ReversalService(
                timeProviderPort, idempotencyPort,
                transactionRepositoryPort, ledgerQueryPort,
                ledgerAppendPort, auditPort
        );
    }

    @Test
    void returnsCachedResultWhenIdempotencyHit() {
        ReversalResult cached = new ReversalResult("rev-tx", "orig-tx");
        when(idempotencyPort.find("idem-1", ReversalResult.class)).thenReturn(Optional.of(cached));

        ReversalCommand cmd = new ReversalCommand(
                "idem-1",
                new ActorContext(ActorType.ADMIN, "admin-actor", Map.of()),
                "orig-tx"
        );

        ReversalResult result = service.execute(cmd);

        assertSame(cached, result);
        verifyNoInteractions(transactionRepositoryPort, ledgerQueryPort, ledgerAppendPort, auditPort);
    }

    @Test
    void reversal_happyPath_invertsLedger_appends_andAudits() {
        Instant now = Instant.parse("2026-01-21T10:15:30Z");
        when(timeProviderPort.now()).thenReturn(now);
        when(idempotencyPort.find("idem-2", ReversalResult.class)).thenReturn(Optional.empty());

        TransactionId origId = TransactionId.of("orig-tx");
        Transaction original = new Transaction(origId, TransactionType.PAY_BY_CARD, Money.of(BigDecimal.valueOf(50)), now, null);
        when(transactionRepositoryPort.findById(origId)).thenReturn(Optional.of(original));

        List<LedgerEntry> originalEntries = List.of(
                new LedgerEntry("e1", origId, LedgerAccount.CLIENT, LedgerEntryType.DEBIT, Money.of(BigDecimal.valueOf(52)), "c-1"),
                new LedgerEntry("e2", origId, LedgerAccount.MERCHANT, LedgerEntryType.CREDIT, Money.of(BigDecimal.valueOf(50)), "m-1"),
                new LedgerEntry("e3", origId, LedgerAccount.PLATFORM, LedgerEntryType.CREDIT, Money.of(BigDecimal.valueOf(2)), null)
        );
        when(ledgerQueryPort.findByTransactionId("orig-tx")).thenReturn(originalEntries);

        when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        ReversalCommand cmd = new ReversalCommand(
                "idem-2",
                new ActorContext(ActorType.ADMIN, "admin-actor", Map.of()),
                "orig-tx"
        );

        ReversalResult result = service.execute(cmd);

        assertNotNull(result.reversalTransactionId());
        assertEquals("orig-tx", result.originalTransactionId());

        ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(ledgerAppendPort).append(captor.capture());
        List<LedgerEntry> reversed = captor.getValue();
        assertEquals(3, reversed.size());

        // Check inversion
        assertTrue(reversed.stream().anyMatch(e ->
                e.account() == LedgerAccount.CLIENT
                        && e.type() == LedgerEntryType.CREDIT
                        && e.amount().equals(Money.of(BigDecimal.valueOf(52)))
                        && "c-1".equals(e.referenceId())
        ));

        assertTrue(reversed.stream().anyMatch(e ->
                e.account() == LedgerAccount.MERCHANT
                        && e.type() == LedgerEntryType.DEBIT
                        && e.amount().equals(Money.of(BigDecimal.valueOf(50)))
                        && "m-1".equals(e.referenceId())
        ));

        assertTrue(reversed.stream().anyMatch(e ->
                e.account() == LedgerAccount.PLATFORM
                        && e.type() == LedgerEntryType.DEBIT
                        && e.amount().equals(Money.of(BigDecimal.valueOf(2)))
                        && e.referenceId() == null
        ));

        verify(auditPort).publish(argThat(ev ->
                ev.action().equals("REVERSAL")
                        && ev.actorType().equals("ADMIN")
                        && ev.actorId().equals("admin-actor")
                        && ev.occurredAt().equals(now)
                        && ev.metadata().get("originalTransactionId").equals("orig-tx")
                        && ev.metadata().containsKey("transactionId")
        ));

        verify(idempotencyPort).save("idem-2", result);
    }

    @Test
    void reversal_forbidden_whenActorNotAdmin() {
        when(idempotencyPort.find("idem-3", ReversalResult.class)).thenReturn(Optional.empty());

        ReversalCommand cmd = new ReversalCommand(
                "idem-3",
                new ActorContext(ActorType.AGENT, "agent-actor", Map.of()),
                "orig-tx"
        );

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd));
    }

    @Test
    void reversal_forbidden_whenOriginalTransactionMissing() {
        when(idempotencyPort.find("idem-4", ReversalResult.class)).thenReturn(Optional.empty());
        when(transactionRepositoryPort.findById(TransactionId.of("orig-404"))).thenReturn(Optional.empty());

        ReversalCommand cmd = new ReversalCommand(
                "idem-4",
                new ActorContext(ActorType.ADMIN, "admin-actor", Map.of()),
                "orig-404"
        );

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd));
    }
}
