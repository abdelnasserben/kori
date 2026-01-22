package com.kori.integration.usecase;

import com.kori.application.command.SearchTransactionHistoryCommand;
import com.kori.application.command.TransactionHistoryView;
import com.kori.application.port.in.SearchTransactionHistoryUseCase;
import com.kori.application.result.TransactionHistoryResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.transaction.TransactionType;
import com.kori.integration.AbstractIntegrationTest;
import com.kori.integration.fixture.LedgerSqlFixture;
import com.kori.integration.fixture.TransactionFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchTransactionHistoryPayByCardViewClientIT extends AbstractIntegrationTest {

    @Autowired SearchTransactionHistoryUseCase searchTransactionHistoryUseCase;

    @Test
    void clientSeesOwnPayByCardTransactions_sortedNewestFirst_withCorrectProjection() {
        // Given
        String clientId = UUID.randomUUID().toString();
        String otherClientId = UUID.randomUUID().toString();

        String merchantId = "MERCHANT_" + UUID.randomUUID();

        OffsetDateTime t1 = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        OffsetDateTime t2 = OffsetDateTime.parse("2026-01-01T11:00:00Z");

        UUID tx1 = UUID.randomUUID();
        UUID tx2 = UUID.randomUUID();
        UUID txOther = UUID.randomUUID();

        TransactionFixture txFixture = new TransactionFixture(transactionJpaRepository);
        LedgerSqlFixture ledgerFixture = new LedgerSqlFixture(jdbcTemplate);

        // Create transactions via JPA (avoid fragile SQL) - identical to original
        txFixture.create(tx1, TransactionType.PAY_BY_CARD.name(), new BigDecimal("100.00"), t1, null);
        txFixture.create(tx2, TransactionType.PAY_BY_CARD.name(), new BigDecimal("50.00"), t2, null);
        txFixture.create(txOther, TransactionType.PAY_BY_CARD.name(), new BigDecimal("999.00"), t2, null);

        // Ledger for tx1: client debited 102.00, merchant credited 100.00, platform credited 2.00
        ledgerFixture.insertEntry(UUID.randomUUID(), tx1, "CLIENT", "DEBIT", new BigDecimal("102.00"), clientId, t1);
        ledgerFixture.insertEntry(UUID.randomUUID(), tx1, "MERCHANT", "CREDIT", new BigDecimal("100.00"), merchantId, t1);
        ledgerFixture.insertEntry(UUID.randomUUID(), tx1, "PLATFORM", "CREDIT", new BigDecimal("2.00"), null, t1);

        // Ledger for tx2: client debited 51.00, merchant credited 50.00, platform credited 1.00
        ledgerFixture.insertEntry(UUID.randomUUID(), tx2, "CLIENT", "DEBIT", new BigDecimal("51.00"), clientId, t2);
        ledgerFixture.insertEntry(UUID.randomUUID(), tx2, "MERCHANT", "CREDIT", new BigDecimal("50.00"), merchantId, t2);
        ledgerFixture.insertEntry(UUID.randomUUID(), tx2, "PLATFORM", "CREDIT", new BigDecimal("1.00"), null, t2);

        // Noise: another client's tx should NOT be visible
        ledgerFixture.insertEntry(UUID.randomUUID(), txOther, "CLIENT", "DEBIT", new BigDecimal("999.00"), otherClientId, t2);
        ledgerFixture.insertEntry(UUID.randomUUID(), txOther, "MERCHANT", "CREDIT", new BigDecimal("990.00"), merchantId, t2);
        ledgerFixture.insertEntry(UUID.randomUUID(), txOther, "PLATFORM", "CREDIT", new BigDecimal("9.00"), null, t2);

        // When
        TransactionHistoryResult result = searchTransactionHistoryUseCase.execute(
                new SearchTransactionHistoryCommand(
                        new ActorContext(ActorType.CLIENT, clientId, Map.of()),
                        null, // ledgerAccount (self scope)
                        null, // referenceId (self scope)
                        TransactionType.PAY_BY_CARD, // filter type
                        null, // from
                        null, // to
                        null, // beforeCreatedAt
                        null, // beforeTransactionId
                        null, // minAmount
                        null, // maxAmount
                        TransactionHistoryView.PAY_BY_CARD_VIEW,
                        50
                )
        );

        // Then: scope is CLIENT/self
        assertEquals("CLIENT", result.ledgerAccount().name());
        assertEquals(clientId, result.referenceId());

        // Only tx1 & tx2 are visible
        assertEquals(2, result.items().size());

        // Sorted newest first => tx2 then tx1
        assertEquals(tx2.toString(), result.items().get(0).transactionId());
        assertEquals(tx1.toString(), result.items().get(1).transactionId());

        // Check projection for tx2 (newest)
        var i2 = result.items().get(0);
        assertEquals(TransactionType.PAY_BY_CARD, i2.transactionType());
        assertEquals(t2.toInstant(), i2.createdAt());

        assertEquals(0, i2.amount().compareTo(new BigDecimal("50.00")));       // merchant credit
        assertEquals(0, i2.fee().compareTo(new BigDecimal("1.00")));           // platform credit
        assertEquals(0, i2.totalDebited().compareTo(new BigDecimal("51.00"))); // client debit

        assertEquals(0, i2.selfTotalCredits().compareTo(new BigDecimal("0.00")));
        assertEquals(0, i2.selfTotalDebits().compareTo(new BigDecimal("51.00")));
        assertEquals(0, i2.selfNet().compareTo(new BigDecimal("-51.00")));

        // Check projection for tx1 (older)
        var i1 = result.items().get(1);
        assertEquals(0, i1.amount().compareTo(new BigDecimal("100.00")));
        assertEquals(0, i1.fee().compareTo(new BigDecimal("2.00")));
        assertEquals(0, i1.totalDebited().compareTo(new BigDecimal("102.00")));
        assertEquals(0, i1.selfNet().compareTo(new BigDecimal("-102.00")));

        // Cursor should point to the last item (stable pagination)
        assertEquals(i1.createdAt(), result.nextBeforeCreatedAt());
        assertEquals(i1.transactionId(), result.nextBeforeTransactionId());
    }
}
