package com.kori.application.usecase;

import com.kori.application.command.SearchTransactionHistoryCommand;
import com.kori.application.command.TransactionHistoryView;
import com.kori.application.port.out.LedgerQueryPort;
import com.kori.application.port.out.TransactionRepositoryPort;
import com.kori.application.result.TransactionHistoryItem;
import com.kori.application.result.TransactionHistoryResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.application.security.LedgerAccessPolicy;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class SearchTransactionHistoryServiceTest {

    @Mock LedgerQueryPort ledgerQueryPort;
    @Mock TransactionRepositoryPort transactionRepositoryPort;

    private static final LedgerAccessPolicy POLICY = new LedgerAccessPolicy();

    private static final ActorContext CLIENT_ACTOR = new ActorContext(ActorType.CLIENT, "C-123", Map.of());

    private static final LedgerAccountRef CLIENT_SCOPE = LedgerAccountRef.client("C-123");
    private static final LedgerAccountRef MERCHANT_SCOPE = LedgerAccountRef.merchant("M-456");
    private static final LedgerAccountRef AGENT_SCOPE = LedgerAccountRef.agentWallet("A-789");
    private static final LedgerAccountRef PLATFORM_FEE = LedgerAccountRef.platformFeeRevenue();

    private static final TransactionId TX_ID = new TransactionId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final Instant CREATED_AT = Instant.parse("2026-01-28T10:15:30Z");

    @Test
    void payByCardView_buildsProjectionAndCursor() {
        SearchTransactionHistoryService service = new SearchTransactionHistoryService(
                ledgerQueryPort,
                transactionRepositoryPort,
                POLICY
        );

        SearchTransactionHistoryCommand command = new SearchTransactionHistoryCommand(
                CLIENT_ACTOR,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                TransactionHistoryView.PAY_BY_CARD_VIEW,
                50
        );

        LedgerEntry clientDebit = LedgerEntry.debit(TX_ID, CLIENT_SCOPE, Money.of(new BigDecimal("52.00")));
        LedgerEntry merchantCredit = LedgerEntry.credit(TX_ID, MERCHANT_SCOPE, Money.of(new BigDecimal("50.00")));
        LedgerEntry platformFee = LedgerEntry.credit(TX_ID, PLATFORM_FEE, Money.of(new BigDecimal("2.00")));
        LedgerEntry agentCredit = LedgerEntry.credit(TX_ID, AGENT_SCOPE, Money.of(new BigDecimal("1.50")));

        when(ledgerQueryPort.findEntries(CLIENT_SCOPE)).thenReturn(List.of(clientDebit));
        when(transactionRepositoryPort.findById(TX_ID)).thenReturn(Optional.of(
                Transaction.payByCard(TX_ID, Money.of(new BigDecimal("50.00")), CREATED_AT)
        ));
        when(ledgerQueryPort.findByTransactionId(TX_ID)).thenReturn(List.of(
                clientDebit,
                merchantCredit,
                platformFee,
                agentCredit
        ));

        TransactionHistoryResult out = service.execute(command);

        assertEquals(CLIENT_SCOPE, out.ledgerAccountRef());
        assertEquals(1, out.items().size());
        assertEquals(CREATED_AT, out.nextBeforeCreatedAt());
        assertEquals(TX_ID.value().toString(), out.nextBeforeTransactionId());

        TransactionHistoryItem item = out.items().get(0);
        assertEquals(TX_ID.value().toString(), item.transactionId());
        assertEquals(CREATED_AT, item.createdAt());
        assertEquals("C-123", item.clientId());
        assertEquals("M-456", item.merchantId());
        assertEquals("A-789", item.agentId());
        assertEquals(new BigDecimal("52.00"), item.selfTotalDebits());
        assertEquals(new BigDecimal("0.00"), item.selfTotalCredits());
        assertEquals(new BigDecimal("-52.00"), item.selfNet());
        assertEquals(new BigDecimal("50.00"), item.amount());
        assertEquals(new BigDecimal("2.00"), item.fee());
        assertEquals(new BigDecimal("52.00"), item.totalDebited());

        verify(ledgerQueryPort).findEntries(CLIENT_SCOPE);
        verify(transactionRepositoryPort).findById(TX_ID);
        verify(ledgerQueryPort).findByTransactionId(TX_ID);
        verifyNoMoreInteractions(ledgerQueryPort, transactionRepositoryPort);
    }

    @Test
    void commissionView_filtersOutWhenNoAgentCredit() {
        SearchTransactionHistoryService service = new SearchTransactionHistoryService(
                ledgerQueryPort,
                transactionRepositoryPort,
                POLICY
        );

        SearchTransactionHistoryCommand command = new SearchTransactionHistoryCommand(
                CLIENT_ACTOR,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                TransactionHistoryView.COMMISSION_VIEW,
                50
        );

        LedgerEntry clientDebit = LedgerEntry.debit(TX_ID, CLIENT_SCOPE, Money.of(new BigDecimal("52.00")));
        LedgerEntry merchantCredit = LedgerEntry.credit(TX_ID, MERCHANT_SCOPE, Money.of(new BigDecimal("50.00")));
        LedgerEntry platformFee = LedgerEntry.credit(TX_ID, PLATFORM_FEE, Money.of(new BigDecimal("2.00")));

        when(ledgerQueryPort.findEntries(CLIENT_SCOPE)).thenReturn(List.of(clientDebit));
        when(transactionRepositoryPort.findById(TX_ID)).thenReturn(Optional.of(
                Transaction.payByCard(TX_ID, Money.of(new BigDecimal("50.00")), CREATED_AT)
        ));
        when(ledgerQueryPort.findByTransactionId(TX_ID)).thenReturn(List.of(
                clientDebit,
                merchantCredit,
                platformFee
        ));

        TransactionHistoryResult out = service.execute(command);

        assertEquals(CLIENT_SCOPE, out.ledgerAccountRef());
        assertEquals(0, out.items().size());
        assertNull(out.nextBeforeCreatedAt());
        assertNull(out.nextBeforeTransactionId());

        verify(ledgerQueryPort).findEntries(CLIENT_SCOPE);
        verify(transactionRepositoryPort).findById(TX_ID);
        verify(ledgerQueryPort).findByTransactionId(TX_ID);
        verifyNoMoreInteractions(ledgerQueryPort, transactionRepositoryPort);
    }
}
