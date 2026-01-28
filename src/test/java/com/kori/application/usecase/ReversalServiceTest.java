package com.kori.application.usecase;

import com.kori.application.command.ReversalCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.out.*;
import com.kori.application.result.ReversalResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.ledger.LedgerEntryType;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.common.Money;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class ReversalServiceTest {

    // ======= mocks =======
    @Mock TimeProviderPort timeProviderPort;
    @Mock IdempotencyPort idempotencyPort;
    @Mock TransactionRepositoryPort transactionRepositoryPort;
    @Mock LedgerQueryPort ledgerQueryPort;
    @Mock LedgerAppendPort ledgerAppendPort;
    @Mock AuditPort auditPort;
    @Mock IdGeneratorPort idGeneratorPort;

    @InjectMocks ReversalService reversalService;

    // ======= constants =======
    private static final String IDEM_KEY = "idem-1";
    private static final String ADMIN_ACTOR_ID = "admin-actor";
    private static final String NON_ADMIN_ACTOR_ID = "agent-actor";

    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static final UUID ORIGINAL_TX_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final TransactionId ORIGINAL_TX_ID = new TransactionId(ORIGINAL_TX_UUID);

    private static final UUID REVERSAL_TX_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final TransactionId REVERSAL_TX_ID = new TransactionId(REVERSAL_TX_UUID);

    private static final Money ORIGINAL_AMOUNT = Money.of(new BigDecimal("52.00"));

    // Ledger accounts (on s’en fiche du format exact ici, juste cohérence)
    private static final LedgerAccountRef CLIENT_ACC = LedgerAccountRef.client("C-1");
    private static final LedgerAccountRef MERCHANT_ACC = LedgerAccountRef.merchant("M-1");
    private static final LedgerAccountRef PLATFORM_FEE_ACC = LedgerAccountRef.platformFeeRevenue();

    // ======= helpers =======
    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, ADMIN_ACTOR_ID, Map.of());
    }

    private static ActorContext nonAdminActor() {
        return new ActorContext(ActorType.AGENT, NON_ADMIN_ACTOR_ID, Map.of());
    }

    private static ReversalCommand cmd(ActorContext actor) {
        return new ReversalCommand(IDEM_KEY, actor, ORIGINAL_TX_UUID.toString());
    }

    private static Transaction originalTx() {
        return Transaction.payByCard(ORIGINAL_TX_ID, ORIGINAL_AMOUNT, NOW.minusSeconds(120));
    }

    // ======= tests =======

    @Test
    void returnsCachedResult_whenIdempotencyKeyAlreadyProcessed() {
        ReversalResult cached = new ReversalResult("tx-1", ORIGINAL_TX_UUID.toString());
        when(idempotencyPort.find(IDEM_KEY, ReversalResult.class)).thenReturn(Optional.of(cached));

        ReversalResult out = reversalService.execute(cmd(adminActor()));

        assertSame(cached, out);
        verify(idempotencyPort).find(IDEM_KEY, ReversalResult.class);

        verifyNoMoreInteractions(
                timeProviderPort,
                transactionRepositoryPort,
                ledgerQueryPort,
                ledgerAppendPort,
                auditPort,
                idGeneratorPort,
                idempotencyPort
        );
    }

    @Test
    void forbidden_whenActorIsNotAdmin() {
        when(idempotencyPort.find(IDEM_KEY, ReversalResult.class)).thenReturn(Optional.empty());

        assertThrows(ForbiddenOperationException.class, () -> reversalService.execute(cmd(nonAdminActor())));

        verify(idempotencyPort).find(IDEM_KEY, ReversalResult.class);
        verifyNoMoreInteractions(idempotencyPort);

        verifyNoInteractions(
                timeProviderPort,
                transactionRepositoryPort,
                ledgerQueryPort,
                ledgerAppendPort,
                auditPort,
                idGeneratorPort
        );
    }

    @Test
    void throwsNotFound_whenOriginalTransactionDoesNotExist() {
        when(idempotencyPort.find(IDEM_KEY, ReversalResult.class)).thenReturn(Optional.empty());
        when(transactionRepositoryPort.findById(ORIGINAL_TX_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reversalService.execute(cmd(adminActor())));

        verify(transactionRepositoryPort).findById(ORIGINAL_TX_ID);
        verifyNoInteractions(ledgerQueryPort, ledgerAppendPort, auditPort, idGeneratorPort);
    }

    @Test
    void forbidden_whenTransactionAlreadyReversed() {
        when(idempotencyPort.find(IDEM_KEY, ReversalResult.class)).thenReturn(Optional.empty());
        when(transactionRepositoryPort.findById(ORIGINAL_TX_ID)).thenReturn(Optional.of(originalTx()));
        when(transactionRepositoryPort.existsReversalFor(ORIGINAL_TX_ID)).thenReturn(true);

        assertThrows(ForbiddenOperationException.class, () -> reversalService.execute(cmd(adminActor())));

        verify(transactionRepositoryPort).existsReversalFor(ORIGINAL_TX_ID);
        verifyNoInteractions(ledgerQueryPort, ledgerAppendPort, auditPort, idGeneratorPort);
    }

    @Test
    void forbidden_whenOriginalTransactionHasNoLedgerEntries() {
        when(idempotencyPort.find(IDEM_KEY, ReversalResult.class)).thenReturn(Optional.empty());
        when(transactionRepositoryPort.findById(ORIGINAL_TX_ID)).thenReturn(Optional.of(originalTx()));
        when(transactionRepositoryPort.existsReversalFor(ORIGINAL_TX_ID)).thenReturn(false);
        when(ledgerQueryPort.findByTransactionId(ORIGINAL_TX_ID)).thenReturn(List.of());

        assertThrows(ForbiddenOperationException.class, () -> reversalService.execute(cmd(adminActor())));

        verify(ledgerQueryPort).findByTransactionId(ORIGINAL_TX_ID);
        verifyNoInteractions(ledgerAppendPort, auditPort, idGeneratorPort);
    }

    @Test
    void happyPath_createsReversalTx_appendsInverseLedger_audits_andSavesIdempotency() {
        when(idempotencyPort.find(IDEM_KEY, ReversalResult.class)).thenReturn(Optional.empty());
        when(transactionRepositoryPort.findById(ORIGINAL_TX_ID)).thenReturn(Optional.of(originalTx()));
        when(transactionRepositoryPort.existsReversalFor(ORIGINAL_TX_ID)).thenReturn(false);

        Money amount = Money.of(new BigDecimal("50.00"));
        Money fee = Money.of(new BigDecimal("2.00"));
        Money total = Money.of(new BigDecimal("52.00"));

        List<LedgerEntry> originalEntries = List.of(
                LedgerEntry.debit(ORIGINAL_TX_ID, CLIENT_ACC, total),
                LedgerEntry.credit(ORIGINAL_TX_ID, MERCHANT_ACC, amount),
                LedgerEntry.credit(ORIGINAL_TX_ID, PLATFORM_FEE_ACC, fee)
        );
        when(ledgerQueryPort.findByTransactionId(ORIGINAL_TX_ID)).thenReturn(originalEntries);

        when(timeProviderPort.now()).thenReturn(NOW);
        when(idGeneratorPort.newUuid()).thenReturn(REVERSAL_TX_UUID);
        when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        ReversalResult out = reversalService.execute(cmd(adminActor()));

        assertEquals(REVERSAL_TX_UUID.toString(), out.reversalTransactionId());
        assertEquals(ORIGINAL_TX_UUID.toString(), out.originalTransactionId());

        // Transaction reversal saved
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort).save(txCaptor.capture());
        Transaction saved = txCaptor.getValue();

        assertEquals(REVERSAL_TX_ID, saved.id());
        assertEquals(ORIGINAL_TX_ID, saved.originalTransactionId());
        assertEquals(ORIGINAL_AMOUNT, saved.amount());

        // Ledger entries inverted
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> ledgerCaptor = ArgumentCaptor.forClass(List.class);
        verify(ledgerAppendPort).append(ledgerCaptor.capture());

        List<LedgerEntry> reversalEntries = ledgerCaptor.getValue();
        assertEquals(3, reversalEntries.size());

        assertTrue(reversalEntries.stream().anyMatch(e ->
                e.transactionId().equals(REVERSAL_TX_ID)
                        && e.type() == LedgerEntryType.CREDIT
                        && e.accountRef().equals(CLIENT_ACC)
                        && e.amount().equals(total)
        ));

        assertTrue(reversalEntries.stream().anyMatch(e ->
                e.transactionId().equals(REVERSAL_TX_ID)
                        && e.type() == LedgerEntryType.DEBIT
                        && e.accountRef().equals(MERCHANT_ACC)
                        && e.amount().equals(amount)
        ));

        assertTrue(reversalEntries.stream().anyMatch(e ->
                e.transactionId().equals(REVERSAL_TX_ID)
                        && e.type() == LedgerEntryType.DEBIT
                        && e.accountRef().equals(PLATFORM_FEE_ACC)
                        && e.amount().equals(fee)
        ));

        // Audit
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());

        AuditEvent event = auditCaptor.getValue();
        assertEquals("REVERSAL", event.action());
        assertEquals("ADMIN", event.actorType());
        assertEquals(ADMIN_ACTOR_ID, event.actorId());
        assertEquals(NOW, event.occurredAt());
        assertEquals(REVERSAL_TX_UUID.toString(), event.metadata().get("transactionId"));
        assertEquals(ORIGINAL_TX_UUID.toString(), event.metadata().get("originalTransactionId"));

        verify(idempotencyPort).save(eq(IDEM_KEY), any(ReversalResult.class));
    }
}
