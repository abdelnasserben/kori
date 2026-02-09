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
import com.kori.domain.model.config.FeeConfig;
import com.kori.domain.model.config.PlatformConfig;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;
import org.junit.jupiter.api.BeforeEach;
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

    @Mock TimeProviderPort timeProviderPort;
    @Mock IdempotencyPort idempotencyPort;
    @Mock TransactionRepositoryPort transactionRepositoryPort;
    @Mock LedgerQueryPort ledgerQueryPort;
    @Mock LedgerAppendPort ledgerAppendPort;
    @Mock AuditPort auditPort;
    @Mock IdGeneratorPort idGeneratorPort;
    @Mock FeeConfigPort feeConfigPort;
    @Mock PlatformConfigPort platformConfigPort;

    @InjectMocks ReversalService reversalService;

    private static final String IDEM_KEY = "idem-1";
    private static final String REQUEST_HASH = "request-hash";
    private static final String ADMIN_ACTOR_ID = "admin-actor";
    private static final String NON_ADMIN_ACTOR_ID = "agent-actor";

    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static final UUID ORIGINAL_TX_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final TransactionId ORIGINAL_TX_ID = new TransactionId(ORIGINAL_TX_UUID);

    private static final UUID REVERSAL_TX_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final Money ORIGINAL_AMOUNT = Money.of(new BigDecimal("52.00"));

    private static final LedgerAccountRef CLIENT_ACC = LedgerAccountRef.client("C-1");
    private static final LedgerAccountRef MERCHANT_ACC = LedgerAccountRef.merchant("M-1");
    private static final LedgerAccountRef PLATFORM_FEE_ACC = LedgerAccountRef.platformFeeRevenue();
    private static final LedgerAccountRef AGENT_CASH_CLEARING_ACC = LedgerAccountRef.agentCashClearing("A-1");
    private static final LedgerAccountRef AGENT_WALLET_ACC = LedgerAccountRef.agentWallet("A-1");

    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, ADMIN_ACTOR_ID, Map.of());
    }

    private static ActorContext nonAdminActor() {
        return new ActorContext(ActorType.AGENT, NON_ADMIN_ACTOR_ID, Map.of());
    }

    private static ReversalCommand cmd(ActorContext actor) {
        return new ReversalCommand(IDEM_KEY, REQUEST_HASH, actor, ORIGINAL_TX_UUID.toString());
    }

    private static Transaction originalCardTx() {
        return Transaction.payByCard(ORIGINAL_TX_ID, ORIGINAL_AMOUNT, NOW.minusSeconds(120));
    }

    @BeforeEach
    void setUp() {
        lenient().when(idempotencyPort.reserve(anyString(), anyString(), any())).thenReturn(true);
    }

    @Test
    void returnsCachedResult_whenIdempotencyKeyAlreadyProcessed() {
        ReversalResult cached = new ReversalResult("tx-1", ORIGINAL_TX_UUID.toString());
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, ReversalResult.class)).thenReturn(Optional.of(cached));

        ReversalResult out = reversalService.execute(cmd(adminActor()));

        assertSame(cached, out);
        verify(idempotencyPort).find(IDEM_KEY, REQUEST_HASH, ReversalResult.class);

        verifyNoMoreInteractions(
                timeProviderPort,
                transactionRepositoryPort,
                ledgerQueryPort,
                ledgerAppendPort,
                auditPort,
                idGeneratorPort,
                feeConfigPort,
                idempotencyPort
        );
    }

    @Test
    void forbidden_whenActorIsNotAdmin() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, ReversalResult.class)).thenReturn(Optional.empty());

        assertThrows(ForbiddenOperationException.class, () -> reversalService.execute(cmd(nonAdminActor())));

        verify(idempotencyPort).find(IDEM_KEY, REQUEST_HASH, ReversalResult.class);
        verifyNoMoreInteractions(idempotencyPort);

        verifyNoInteractions(
                timeProviderPort,
                transactionRepositoryPort,
                ledgerQueryPort,
                ledgerAppendPort,
                auditPort,
                idGeneratorPort,
                feeConfigPort
        );
    }

    @Test
    void throwsNotFound_whenOriginalTransactionDoesNotExist() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, ReversalResult.class)).thenReturn(Optional.empty());
        when(transactionRepositoryPort.findById(ORIGINAL_TX_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reversalService.execute(cmd(adminActor())));

        verify(transactionRepositoryPort).findById(ORIGINAL_TX_ID);
        verifyNoInteractions(ledgerQueryPort, ledgerAppendPort, auditPort, idGeneratorPort, feeConfigPort);
    }

    @Test
    void forbidden_whenTransactionAlreadyReversed() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, ReversalResult.class)).thenReturn(Optional.empty());
        when(transactionRepositoryPort.findById(ORIGINAL_TX_ID)).thenReturn(Optional.of(originalCardTx()));
        when(transactionRepositoryPort.existsReversalFor(ORIGINAL_TX_ID)).thenReturn(true);

        assertThrows(ForbiddenOperationException.class, () -> reversalService.execute(cmd(adminActor())));

        verify(transactionRepositoryPort).existsReversalFor(ORIGINAL_TX_ID);
        verifyNoInteractions(ledgerQueryPort, ledgerAppendPort, auditPort, idGeneratorPort, feeConfigPort);
    }

    @Test
    void forbidden_whenOriginalTransactionHasNoLedgerEntries() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, ReversalResult.class)).thenReturn(Optional.empty());
        when(transactionRepositoryPort.findById(ORIGINAL_TX_ID)).thenReturn(Optional.of(originalCardTx()));
        when(transactionRepositoryPort.existsReversalFor(ORIGINAL_TX_ID)).thenReturn(false);
        when(ledgerQueryPort.findByTransactionId(ORIGINAL_TX_ID)).thenReturn(List.of());

        assertThrows(ForbiddenOperationException.class, () -> reversalService.execute(cmd(adminActor())));

        verify(ledgerQueryPort).findByTransactionId(ORIGINAL_TX_ID);
        verifyNoInteractions(ledgerAppendPort, auditPort, idGeneratorPort, feeConfigPort);
    }

    @Test
    void payByCard_refundableFalse_reversesPrincipalOnly() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, ReversalResult.class)).thenReturn(Optional.empty());
        when(transactionRepositoryPort.findById(ORIGINAL_TX_ID)).thenReturn(Optional.of(originalCardTx()));
        when(transactionRepositoryPort.existsReversalFor(ORIGINAL_TX_ID)).thenReturn(false);
        when(feeConfigPort.get()).thenReturn(Optional.of(new FeeConfig(
                new BigDecimal("10.00"), new BigDecimal("0.01"), new BigDecimal("1.00"), new BigDecimal("10.00"),
                new BigDecimal("0.01"), new BigDecimal("1.00"), new BigDecimal("10.00"), false, true, false
        )));

        Money amount = Money.of(new BigDecimal("50.00"));
        Money fee = Money.of(new BigDecimal("2.00"));

        List<LedgerEntry> originalEntries = List.of(
                LedgerEntry.debit(ORIGINAL_TX_ID, CLIENT_ACC, amount.plus(fee)),
                LedgerEntry.credit(ORIGINAL_TX_ID, MERCHANT_ACC, amount),
                LedgerEntry.credit(ORIGINAL_TX_ID, PLATFORM_FEE_ACC, fee)
        );
        when(ledgerQueryPort.findByTransactionId(ORIGINAL_TX_ID)).thenReturn(originalEntries);

        when(timeProviderPort.now()).thenReturn(NOW);
        when(idGeneratorPort.newUuid()).thenReturn(REVERSAL_TX_UUID);

        reversalService.execute(cmd(adminActor()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(ledgerAppendPort).append(captor.capture());
        List<LedgerEntry> reversalEntries = captor.getValue();

        assertEquals(2, reversalEntries.size());
        assertTrue(reversalEntries.stream().anyMatch(e -> e.type() == LedgerEntryType.DEBIT && e.accountRef().equals(MERCHANT_ACC) && e.amount().equals(amount)));
        assertTrue(reversalEntries.stream().anyMatch(e -> e.type() == LedgerEntryType.CREDIT && e.accountRef().equals(CLIENT_ACC) && e.amount().equals(amount)));
        assertFalse(reversalEntries.stream().anyMatch(e -> e.accountRef().equals(PLATFORM_FEE_ACC)));
    }

    @Test
    void payByCard_refundableTrue_reversesPrincipalAndFee() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, ReversalResult.class)).thenReturn(Optional.empty());
        when(transactionRepositoryPort.findById(ORIGINAL_TX_ID)).thenReturn(Optional.of(originalCardTx()));
        when(transactionRepositoryPort.existsReversalFor(ORIGINAL_TX_ID)).thenReturn(false);
        when(feeConfigPort.get()).thenReturn(Optional.of(new FeeConfig(
                new BigDecimal("10.00"), new BigDecimal("0.01"), new BigDecimal("1.00"), new BigDecimal("10.00"),
                new BigDecimal("0.01"), new BigDecimal("1.00"), new BigDecimal("10.00"), true, true, false
        )));

        Money amount = Money.of(new BigDecimal("50.00"));
        Money fee = Money.of(new BigDecimal("2.00"));
        List<LedgerEntry> originalEntries = List.of(
                LedgerEntry.debit(ORIGINAL_TX_ID, CLIENT_ACC, amount.plus(fee)),
                LedgerEntry.credit(ORIGINAL_TX_ID, MERCHANT_ACC, amount),
                LedgerEntry.credit(ORIGINAL_TX_ID, PLATFORM_FEE_ACC, fee)
        );
        when(ledgerQueryPort.findByTransactionId(ORIGINAL_TX_ID)).thenReturn(originalEntries);
        when(timeProviderPort.now()).thenReturn(NOW);
        when(idGeneratorPort.newUuid()).thenReturn(REVERSAL_TX_UUID);

        ReversalResult out = reversalService.execute(cmd(adminActor()));
        assertEquals(REVERSAL_TX_UUID.toString(), out.reversalTransactionId());


        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(ledgerAppendPort).append(captor.capture());
        List<LedgerEntry> reversalEntries = captor.getValue();

        assertEquals(3, reversalEntries.size());

        assertTrue(reversalEntries.stream().anyMatch(e -> e.type() == LedgerEntryType.DEBIT && e.accountRef().equals(MERCHANT_ACC) && e.amount().equals(amount)));
        assertTrue(reversalEntries.stream().anyMatch(e -> e.type() == LedgerEntryType.DEBIT && e.accountRef().equals(PLATFORM_FEE_ACC) && e.amount().equals(fee)));
        assertTrue(reversalEntries.stream().anyMatch(e -> e.type() == LedgerEntryType.CREDIT && e.accountRef().equals(CLIENT_ACC) && e.amount().equals(amount.plus(fee))));

        // Audit
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());

        verify(idempotencyPort).save(eq(IDEM_KEY), eq(REQUEST_HASH), any(ReversalResult.class));
    }

    @Test
    void merchantWithdraw_refundableTrue_reversesPrincipalAndFeeRevenue() {
        Transaction withdrawTx = Transaction.merchantWithdrawAtAgent(ORIGINAL_TX_ID, Money.of(new BigDecimal("100.00")), NOW.minusSeconds(60));
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, ReversalResult.class)).thenReturn(Optional.empty());
        when(transactionRepositoryPort.findById(ORIGINAL_TX_ID)).thenReturn(Optional.of(withdrawTx));
        when(transactionRepositoryPort.existsReversalFor(ORIGINAL_TX_ID)).thenReturn(false);
        when(feeConfigPort.get()).thenReturn(Optional.of(new FeeConfig(
                new BigDecimal("10.00"), new BigDecimal("0.01"), new BigDecimal("1.00"), new BigDecimal("10.00"),
                new BigDecimal("0.01"), new BigDecimal("1.00"), new BigDecimal("10.00"), false, true, false
        )));

        when(platformConfigPort.get()).thenReturn(Optional.of(new PlatformConfig(new BigDecimal("1000.00"))));
        when(ledgerQueryPort.getBalance(AGENT_CASH_CLEARING_ACC)).thenReturn(Money.of(new BigDecimal("0.00")));

        Money principal = Money.of(new BigDecimal("100.00"));
        Money feeRevenue = Money.of(new BigDecimal("3.00"));
        List<LedgerEntry> originalEntries = List.of(
                LedgerEntry.debit(ORIGINAL_TX_ID, MERCHANT_ACC, principal.plus(feeRevenue)),
                LedgerEntry.credit(ORIGINAL_TX_ID, AGENT_CASH_CLEARING_ACC, principal),
                LedgerEntry.credit(ORIGINAL_TX_ID, PLATFORM_FEE_ACC, feeRevenue)
        );
        when(ledgerQueryPort.findByTransactionId(ORIGINAL_TX_ID)).thenReturn(originalEntries);
        when(timeProviderPort.now()).thenReturn(NOW);
        when(idGeneratorPort.newUuid()).thenReturn(REVERSAL_TX_UUID);

        reversalService.execute(cmd(adminActor()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(ledgerAppendPort).append(captor.capture());
        List<LedgerEntry> reversalEntries = captor.getValue();

        assertTrue(reversalEntries.stream().anyMatch(e -> e.type() == LedgerEntryType.DEBIT && e.accountRef().equals(AGENT_CASH_CLEARING_ACC) && e.amount().equals(principal)));
        assertTrue(reversalEntries.stream().anyMatch(e -> e.type() == LedgerEntryType.DEBIT && e.accountRef().equals(PLATFORM_FEE_ACC) && e.amount().equals(feeRevenue)));
        assertTrue(reversalEntries.stream().anyMatch(e -> e.type() == LedgerEntryType.CREDIT && e.accountRef().equals(MERCHANT_ACC) && e.amount().equals(principal.plus(feeRevenue))));
    }

    @Test
    void enrollCard_refundableTrue_reversesWalletAndPlatformFeeToCashClearing() {
        Transaction enrollTx = Transaction.enrollCard(ORIGINAL_TX_ID, Money.of(new BigDecimal("10.00")), NOW.minusSeconds(60));
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, ReversalResult.class)).thenReturn(Optional.empty());
        when(transactionRepositoryPort.findById(ORIGINAL_TX_ID)).thenReturn(Optional.of(enrollTx));
        when(transactionRepositoryPort.existsReversalFor(ORIGINAL_TX_ID)).thenReturn(false);
        when(feeConfigPort.get()).thenReturn(Optional.of(new FeeConfig(
                new BigDecimal("10.00"), new BigDecimal("0.01"), new BigDecimal("1.00"), new BigDecimal("10.00"),
                new BigDecimal("0.01"), new BigDecimal("1.00"), new BigDecimal("10.00"), false, true, true
        )));
        when(platformConfigPort.get()).thenReturn(Optional.of(new com.kori.domain.model.config.PlatformConfig(new BigDecimal("1000.00"))));
        when(ledgerQueryPort.getBalance(AGENT_CASH_CLEARING_ACC)).thenReturn(Money.of(new BigDecimal("-5.00")));

        Money wallet = Money.of(new BigDecimal("3.00"));
        Money fee = Money.of(new BigDecimal("7.00"));
        List<LedgerEntry> originalEntries = List.of(
                LedgerEntry.debit(ORIGINAL_TX_ID, AGENT_CASH_CLEARING_ACC, wallet.plus(fee)),
                LedgerEntry.credit(ORIGINAL_TX_ID, AGENT_WALLET_ACC, wallet),
                LedgerEntry.credit(ORIGINAL_TX_ID, PLATFORM_FEE_ACC, fee)
        );
        when(ledgerQueryPort.findByTransactionId(ORIGINAL_TX_ID)).thenReturn(originalEntries);
        when(timeProviderPort.now()).thenReturn(NOW);
        when(idGeneratorPort.newUuid()).thenReturn(REVERSAL_TX_UUID);

        reversalService.execute(cmd(adminActor()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(ledgerAppendPort).append(captor.capture());
        List<LedgerEntry> reversalEntries = captor.getValue();

        assertEquals(3, reversalEntries.size());
        assertTrue(reversalEntries.stream().anyMatch(e -> e.type() == LedgerEntryType.DEBIT && e.accountRef().equals(AGENT_WALLET_ACC) && e.amount().equals(wallet)));
        assertTrue(reversalEntries.stream().anyMatch(e -> e.type() == LedgerEntryType.DEBIT && e.accountRef().equals(PLATFORM_FEE_ACC) && e.amount().equals(fee)));
        assertTrue(reversalEntries.stream().anyMatch(e -> e.type() == LedgerEntryType.CREDIT && e.accountRef().equals(AGENT_CASH_CLEARING_ACC) && e.amount().equals(wallet.plus(fee))));
    }

    @Test
    void cashReversalBlocked_whenProjectedBalanceBelowGlobalLimit() {
        Transaction withdrawTx = Transaction.merchantWithdrawAtAgent(ORIGINAL_TX_ID, Money.of(new BigDecimal("100.00")), NOW.minusSeconds(60));
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, ReversalResult.class)).thenReturn(Optional.empty());
        when(transactionRepositoryPort.findById(ORIGINAL_TX_ID)).thenReturn(Optional.of(withdrawTx));
        when(transactionRepositoryPort.existsReversalFor(ORIGINAL_TX_ID)).thenReturn(false);
        when(platformConfigPort.get()).thenReturn(Optional.of(new com.kori.domain.model.config.PlatformConfig(new BigDecimal("100.00"))));
        when(ledgerQueryPort.getBalance(AGENT_CASH_CLEARING_ACC)).thenReturn(Money.of(new BigDecimal("-50.00")));

        List<LedgerEntry> originalEntries = List.of(
                LedgerEntry.debit(ORIGINAL_TX_ID, MERCHANT_ACC, Money.of(new BigDecimal("103.00"))),
                LedgerEntry.credit(ORIGINAL_TX_ID, AGENT_CASH_CLEARING_ACC, Money.of(new BigDecimal("100.00"))),
                LedgerEntry.credit(ORIGINAL_TX_ID, PLATFORM_FEE_ACC, Money.of(new BigDecimal("3.00")))
        );
        when(ledgerQueryPort.findByTransactionId(ORIGINAL_TX_ID)).thenReturn(originalEntries);

        ForbiddenOperationException ex = assertThrows(ForbiddenOperationException.class, () -> reversalService.execute(cmd(adminActor())));

        assertTrue(ex.getMessage().contains("Agent cash limit exceeded"));
        verify(transactionRepositoryPort, never()).save(any());
        verifyNoInteractions(ledgerAppendPort);
        verify(idempotencyPort, never()).save(any(), any(), any());
    }
}
