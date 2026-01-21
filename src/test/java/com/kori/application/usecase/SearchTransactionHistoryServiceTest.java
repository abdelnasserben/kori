package com.kori.application.usecase;

import com.kori.application.command.SearchTransactionHistoryCommand;
import com.kori.application.command.TransactionHistoryView;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.out.LedgerQueryPort;
import com.kori.application.port.out.TransactionRepositoryPort;
import com.kori.application.result.TransactionHistoryResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.application.security.LedgerAccessPolicy;
import com.kori.domain.ledger.LedgerAccount;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;
import com.kori.domain.model.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class SearchTransactionHistoryServiceTest {

    @Mock
    LedgerQueryPort ledgerQueryPort;

    @Mock
    TransactionRepositoryPort transactionRepositoryPort;

    private SearchTransactionHistoryService service;

    @BeforeEach
    void setUp() {
        service = new SearchTransactionHistoryService(
                ledgerQueryPort,
                transactionRepositoryPort,
                new LedgerAccessPolicy()
        );
    }

    @Test
    void client_payByCardView_buildsAmountFeeTotalDebited_andCounterparties() {
        var actor = new ActorContext(ActorType.CLIENT, "c-1", Map.of());

        when(ledgerQueryPort.findEntries(LedgerAccount.CLIENT, "c-1"))
                .thenReturn(List.of(
                        LedgerEntry.debit(TransactionId.of("tx-1"), LedgerAccount.CLIENT, Money.of(new BigDecimal("102.00")), "c-1")
                ));

        var txCreatedAt = Instant.parse("2026-01-21T10:00:00Z");
        when(transactionRepositoryPort.findById(TransactionId.of("tx-1")))
                .thenReturn(Optional.of(new Transaction(
                        TransactionId.of("tx-1"),
                        TransactionType.PAY_BY_CARD,
                        Money.of(new BigDecimal("100.00")),
                        txCreatedAt,
                        null
                )));

        when(ledgerQueryPort.findByTransactionId("tx-1"))
                .thenReturn(List.of(
                        LedgerEntry.debit(TransactionId.of("tx-1"), LedgerAccount.CLIENT, Money.of(new BigDecimal("102.00")), "c-1"),
                        LedgerEntry.credit(TransactionId.of("tx-1"), LedgerAccount.MERCHANT, Money.of(new BigDecimal("100.00")), "m-9"),
                        LedgerEntry.credit(TransactionId.of("tx-1"), LedgerAccount.PLATFORM, Money.of(new BigDecimal("2.00")), "platform")
                ));

        TransactionHistoryResult result = service.execute(new SearchTransactionHistoryCommand(
                actor,
                null, // ledgerAccount
                null, // referenceId
                TransactionType.PAY_BY_CARD,
                null, // from
                null, // to
                null, // beforeCreatedAt
                null, // beforeTransactionId
                null, // minAmount
                null, // maxAmount
                TransactionHistoryView.PAY_BY_CARD_VIEW,
                20
        ));

        assertEquals(LedgerAccount.CLIENT, result.ledgerAccount());
        assertEquals("c-1", result.referenceId());
        assertEquals(1, result.items().size());

        var item = result.items().get(0);
        assertEquals("tx-1", item.transactionId());
        assertEquals(TransactionType.PAY_BY_CARD, item.transactionType());

        assertEquals("c-1", item.clientId());
        assertEquals("m-9", item.merchantId());
        assertNull(item.agentId());

        assertEquals(new BigDecimal("100.00"), item.amount());
        assertEquals(new BigDecimal("2.00"), item.fee());
        assertEquals(new BigDecimal("102.00"), item.totalDebited());

        assertEquals(txCreatedAt, result.nextBeforeCreatedAt());
        assertEquals("tx-1", result.nextBeforeTransactionId());
    }

    @Test
    void pagination_cursorComposite_returnsStablePages_noDuplicates() {
        var actor = new ActorContext(ActorType.CLIENT, "c-1", Map.of());

        when(ledgerQueryPort.findEntries(LedgerAccount.CLIENT, "c-1"))
                .thenReturn(List.of(
                        LedgerEntry.debit(TransactionId.of("tx-new"), LedgerAccount.CLIENT, Money.of(new BigDecimal("10.00")), "c-1"),
                        LedgerEntry.debit(TransactionId.of("tx-old"), LedgerAccount.CLIENT, Money.of(new BigDecimal("20.00")), "c-1")
                ));

        var newAt = Instant.parse("2026-01-21T12:00:00Z");
        var oldAt = Instant.parse("2026-01-21T09:00:00Z");

        when(transactionRepositoryPort.findById(TransactionId.of("tx-new")))
                .thenReturn(Optional.of(new Transaction(
                        TransactionId.of("tx-new"),
                        TransactionType.PAY_BY_CARD,
                        Money.of(new BigDecimal("10.00")),
                        newAt,
                        null
                )));

        when(transactionRepositoryPort.findById(TransactionId.of("tx-old")))
                .thenReturn(Optional.of(new Transaction(
                        TransactionId.of("tx-old"),
                        TransactionType.PAY_BY_CARD,
                        Money.of(new BigDecimal("20.00")),
                        oldAt,
                        null
                )));

        when(ledgerQueryPort.findByTransactionId("tx-new"))
                .thenReturn(List.of(
                        LedgerEntry.debit(TransactionId.of("tx-new"), LedgerAccount.CLIENT, Money.of(new BigDecimal("10.00")), "c-1"),
                        LedgerEntry.credit(TransactionId.of("tx-new"), LedgerAccount.MERCHANT, Money.of(new BigDecimal("10.00")), "m-1")
                ));

        when(ledgerQueryPort.findByTransactionId("tx-old"))
                .thenReturn(List.of(
                        LedgerEntry.debit(TransactionId.of("tx-old"), LedgerAccount.CLIENT, Money.of(new BigDecimal("20.00")), "c-1"),
                        LedgerEntry.credit(TransactionId.of("tx-old"), LedgerAccount.MERCHANT, Money.of(new BigDecimal("20.00")), "m-1")
                ));

        var page1 = service.execute(new SearchTransactionHistoryCommand(
                actor,
                null,
                null,
                TransactionType.PAY_BY_CARD,
                null,
                null,
                null,
                null,
                null,
                null,
                TransactionHistoryView.PAY_BY_CARD_VIEW,
                1
        ));

        assertEquals(1, page1.items().size());
        assertEquals("tx-new", page1.items().get(0).transactionId());
        assertEquals(newAt, page1.nextBeforeCreatedAt());
        assertEquals("tx-new", page1.nextBeforeTransactionId());

        var page2 = service.execute(new SearchTransactionHistoryCommand(
                actor,
                null,
                null,
                TransactionType.PAY_BY_CARD,
                null,
                null,
                page1.nextBeforeCreatedAt(),
                page1.nextBeforeTransactionId(),
                null,
                null,
                TransactionHistoryView.PAY_BY_CARD_VIEW,
                10
        ));

        assertEquals(1, page2.items().size());
        assertEquals("tx-old", page2.items().get(0).transactionId());
        assertEquals(oldAt, page2.nextBeforeCreatedAt());
        assertEquals("tx-old", page2.nextBeforeTransactionId());
    }

    @Test
    void pagination_cursorComposite_handlesSameCreatedAt_usingTransactionIdTieBreaker() {
        var actor = new ActorContext(ActorType.CLIENT, "c-1", Map.of());

        var sameAt = Instant.parse("2026-01-21T12:00:00Z");

        when(ledgerQueryPort.findEntries(LedgerAccount.CLIENT, "c-1"))
                .thenReturn(List.of(
                        LedgerEntry.debit(TransactionId.of("tx-2"), LedgerAccount.CLIENT, Money.of(new BigDecimal("10.00")), "c-1"),
                        LedgerEntry.debit(TransactionId.of("tx-1"), LedgerAccount.CLIENT, Money.of(new BigDecimal("10.00")), "c-1")
                ));

        when(transactionRepositoryPort.findById(TransactionId.of("tx-2")))
                .thenReturn(Optional.of(new Transaction(TransactionId.of("tx-2"), TransactionType.PAY_BY_CARD, Money.of(new BigDecimal("10.00")), sameAt, null)));
        when(transactionRepositoryPort.findById(TransactionId.of("tx-1")))
                .thenReturn(Optional.of(new Transaction(TransactionId.of("tx-1"), TransactionType.PAY_BY_CARD, Money.of(new BigDecimal("10.00")), sameAt, null)));

        when(ledgerQueryPort.findByTransactionId("tx-2"))
                .thenReturn(List.of(
                        LedgerEntry.debit(TransactionId.of("tx-2"), LedgerAccount.CLIENT, Money.of(new BigDecimal("10.00")), "c-1"),
                        LedgerEntry.credit(TransactionId.of("tx-2"), LedgerAccount.MERCHANT, Money.of(new BigDecimal("10.00")), "m-1")
                ));

        when(ledgerQueryPort.findByTransactionId("tx-1"))
                .thenReturn(List.of(
                        LedgerEntry.debit(TransactionId.of("tx-1"), LedgerAccount.CLIENT, Money.of(new BigDecimal("10.00")), "c-1"),
                        LedgerEntry.credit(TransactionId.of("tx-1"), LedgerAccount.MERCHANT, Money.of(new BigDecimal("10.00")), "m-1")
                ));

        var page1 = service.execute(new SearchTransactionHistoryCommand(
                actor,
                null,
                null,
                TransactionType.PAY_BY_CARD,
                null,
                null,
                null,
                null,
                null,
                null,
                TransactionHistoryView.PAY_BY_CARD_VIEW,
                1
        ));

        assertEquals(1, page1.items().size());
        assertEquals("tx-2", page1.items().get(0).transactionId());
        assertEquals(sameAt, page1.nextBeforeCreatedAt());
        assertEquals("tx-2", page1.nextBeforeTransactionId());

        var page2 = service.execute(new SearchTransactionHistoryCommand(
                actor,
                null,
                null,
                TransactionType.PAY_BY_CARD,
                null,
                null,
                page1.nextBeforeCreatedAt(),
                page1.nextBeforeTransactionId(),
                null,
                null,
                TransactionHistoryView.PAY_BY_CARD_VIEW,
                10
        ));

        assertEquals(1, page2.items().size());
        assertEquals("tx-1", page2.items().get(0).transactionId());
        assertEquals(sameAt, page2.nextBeforeCreatedAt());
        assertEquals("tx-1", page2.nextBeforeTransactionId());
    }

    @Test
    void filter_transactionType_excludesOtherTypes() {
        var actor = new ActorContext(ActorType.CLIENT, "c-1", Map.of());

        when(ledgerQueryPort.findEntries(LedgerAccount.CLIENT, "c-1"))
                .thenReturn(List.of(
                        LedgerEntry.debit(TransactionId.of("tx-pay"), LedgerAccount.CLIENT, Money.of(new BigDecimal("5.00")), "c-1"),
                        LedgerEntry.debit(TransactionId.of("tx-rev"), LedgerAccount.CLIENT, Money.of(new BigDecimal("5.00")), "c-1")
                ));

        when(transactionRepositoryPort.findById(TransactionId.of("tx-pay")))
                .thenReturn(Optional.of(new Transaction(
                        TransactionId.of("tx-pay"),
                        TransactionType.PAY_BY_CARD,
                        Money.of(new BigDecimal("5.00")),
                        Instant.parse("2026-01-21T10:00:00Z"),
                        null
                )));

        when(transactionRepositoryPort.findById(TransactionId.of("tx-rev")))
                .thenReturn(Optional.of(new Transaction(
                        TransactionId.of("tx-rev"),
                        TransactionType.REVERSAL,
                        Money.of(new BigDecimal("5.00")),
                        Instant.parse("2026-01-21T11:00:00Z"),
                        TransactionId.of("tx-pay")
                )));

        when(ledgerQueryPort.findByTransactionId("tx-pay"))
                .thenReturn(List.of(
                        LedgerEntry.debit(TransactionId.of("tx-pay"), LedgerAccount.CLIENT, Money.of(new BigDecimal("5.00")), "c-1"),
                        LedgerEntry.credit(TransactionId.of("tx-pay"), LedgerAccount.MERCHANT, Money.of(new BigDecimal("5.00")), "m-1")
                ));

        var res = service.execute(new SearchTransactionHistoryCommand(
                actor,
                null,
                null,
                TransactionType.PAY_BY_CARD,
                null,
                null,
                null,
                null,
                null,
                null,
                TransactionHistoryView.SUMMARY,
                50
        ));

        assertEquals(1, res.items().size());
        assertEquals("tx-pay", res.items().get(0).transactionId());

        verify(ledgerQueryPort, never()).findByTransactionId("tx-rev");
    }

    @Test
    void nonAdmin_cannotQueryArbitraryScope() {
        var actor = new ActorContext(ActorType.CLIENT, "c-1", Map.of());

        assertThrows(ForbiddenOperationException.class, () -> service.execute(new SearchTransactionHistoryCommand(
                actor,
                LedgerAccount.MERCHANT,
                "m-9",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                TransactionHistoryView.SUMMARY,
                50
        )));
    }

    @Test
    void admin_canQueryArbitraryScope() {
        var admin = new ActorContext(ActorType.ADMIN, "admin-1", Map.of());

        when(ledgerQueryPort.findEntries(LedgerAccount.MERCHANT, "m-9"))
                .thenReturn(List.of(
                        LedgerEntry.credit(TransactionId.of("tx-1"), LedgerAccount.MERCHANT, Money.of(new BigDecimal("100.00")), "m-9")
                ));

        when(transactionRepositoryPort.findById(TransactionId.of("tx-1")))
                .thenReturn(Optional.of(new Transaction(
                        TransactionId.of("tx-1"),
                        TransactionType.PAY_BY_CARD,
                        Money.of(new BigDecimal("100.00")),
                        Instant.parse("2026-01-21T10:00:00Z"),
                        null
                )));

        when(ledgerQueryPort.findByTransactionId("tx-1"))
                .thenReturn(List.of(
                        LedgerEntry.debit(TransactionId.of("tx-1"), LedgerAccount.CLIENT, Money.of(new BigDecimal("102.00")), "c-1"),
                        LedgerEntry.credit(TransactionId.of("tx-1"), LedgerAccount.MERCHANT, Money.of(new BigDecimal("100.00")), "m-9"),
                        LedgerEntry.credit(TransactionId.of("tx-1"), LedgerAccount.PLATFORM, Money.of(new BigDecimal("2.00")), "platform")
                ));

        var res = service.execute(new SearchTransactionHistoryCommand(
                admin,
                LedgerAccount.MERCHANT,
                "m-9",
                TransactionType.PAY_BY_CARD,
                null,
                null,
                null,
                null,
                null,
                null,
                TransactionHistoryView.PAY_BY_CARD_VIEW,
                50
        ));

        assertEquals(LedgerAccount.MERCHANT, res.ledgerAccount());
        assertEquals("m-9", res.referenceId());
        assertEquals(1, res.items().size());
        assertEquals("tx-1", res.items().get(0).transactionId());
    }

    @Test
    void minAmount_filtersOutSmaller_payByCardView() {
        var actor = new ActorContext(ActorType.CLIENT, "c-1", Map.of());

        when(ledgerQueryPort.findEntries(LedgerAccount.CLIENT, "c-1"))
                .thenReturn(List.of(
                        LedgerEntry.debit(TransactionId.of("tx-1"), LedgerAccount.CLIENT, Money.of(new BigDecimal("6.00")), "c-1")
                ));

        when(transactionRepositoryPort.findById(TransactionId.of("tx-1")))
                .thenReturn(Optional.of(new Transaction(
                        TransactionId.of("tx-1"),
                        TransactionType.PAY_BY_CARD,
                        Money.of(new BigDecimal("5.00")),
                        Instant.parse("2026-01-21T10:00:00Z"),
                        null
                )));

        when(ledgerQueryPort.findByTransactionId("tx-1"))
                .thenReturn(List.of(
                        LedgerEntry.debit(TransactionId.of("tx-1"), LedgerAccount.CLIENT, Money.of(new BigDecimal("6.00")), "c-1"),
                        LedgerEntry.credit(TransactionId.of("tx-1"), LedgerAccount.MERCHANT, Money.of(new BigDecimal("5.00")), "m-1"),
                        LedgerEntry.credit(TransactionId.of("tx-1"), LedgerAccount.PLATFORM, Money.of(new BigDecimal("1.00")), "platform")
                ));

        var res = service.execute(new SearchTransactionHistoryCommand(
                actor,
                null,
                null,
                TransactionType.PAY_BY_CARD,
                null,
                null,
                null,
                null,
                new BigDecimal("10.00"),
                null,
                TransactionHistoryView.PAY_BY_CARD_VIEW,
                50
        ));

        assertTrue(res.items().isEmpty());
    }

    @Test
    void maxAmount_filtersOutLarger_summaryView_usesAbsSelfNet() {
        var actor = new ActorContext(ActorType.CLIENT, "c-1", Map.of());

        when(ledgerQueryPort.findEntries(LedgerAccount.CLIENT, "c-1"))
                .thenReturn(List.of(
                        LedgerEntry.debit(TransactionId.of("tx-1"), LedgerAccount.CLIENT, Money.of(new BigDecimal("120.00")), "c-1")
                ));

        when(transactionRepositoryPort.findById(TransactionId.of("tx-1")))
                .thenReturn(Optional.of(new Transaction(
                        TransactionId.of("tx-1"),
                        TransactionType.PAY_BY_CARD,
                        Money.of(new BigDecimal("120.00")),
                        Instant.parse("2026-01-21T10:00:00Z"),
                        null
                )));

        when(ledgerQueryPort.findByTransactionId("tx-1"))
                .thenReturn(List.of(
                        LedgerEntry.debit(TransactionId.of("tx-1"), LedgerAccount.CLIENT, Money.of(new BigDecimal("120.00")), "c-1")
                ));

        var res = service.execute(new SearchTransactionHistoryCommand(
                actor,
                null,
                null,
                TransactionType.PAY_BY_CARD,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("100.00"),
                TransactionHistoryView.SUMMARY,
                50
        ));

        assertTrue(res.items().isEmpty());
    }

    @Test
    void minMax_inclusive_allowsAmountWithinRange_payByCardView() {
        var actor = new ActorContext(ActorType.CLIENT, "c-1", Map.of());

        when(ledgerQueryPort.findEntries(LedgerAccount.CLIENT, "c-1"))
                .thenReturn(List.of(
                        LedgerEntry.debit(TransactionId.of("tx-1"), LedgerAccount.CLIENT, Money.of(new BigDecimal("102.00")), "c-1")
                ));

        when(transactionRepositoryPort.findById(TransactionId.of("tx-1")))
                .thenReturn(Optional.of(new Transaction(
                        TransactionId.of("tx-1"),
                        TransactionType.PAY_BY_CARD,
                        Money.of(new BigDecimal("100.00")),
                        Instant.parse("2026-01-21T10:00:00Z"),
                        null
                )));

        when(ledgerQueryPort.findByTransactionId("tx-1"))
                .thenReturn(List.of(
                        LedgerEntry.debit(TransactionId.of("tx-1"), LedgerAccount.CLIENT, Money.of(new BigDecimal("102.00")), "c-1"),
                        LedgerEntry.credit(TransactionId.of("tx-1"), LedgerAccount.MERCHANT, Money.of(new BigDecimal("100.00")), "m-9"),
                        LedgerEntry.credit(TransactionId.of("tx-1"), LedgerAccount.PLATFORM, Money.of(new BigDecimal("2.00")), "platform")
                ));

        var res = service.execute(new SearchTransactionHistoryCommand(
                actor,
                null,
                null,
                TransactionType.PAY_BY_CARD,
                null,
                null,
                null,
                null,
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                TransactionHistoryView.PAY_BY_CARD_VIEW,
                50
        ));

        assertEquals(1, res.items().size());
        assertEquals(new BigDecimal("100.00"), res.items().get(0).amount());
    }
}
