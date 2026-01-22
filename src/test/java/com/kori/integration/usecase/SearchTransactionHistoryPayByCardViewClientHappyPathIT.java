package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.TransactionEntity;
import com.kori.adapters.out.jpa.repo.TransactionJpaRepository;
import com.kori.application.command.SearchTransactionHistoryCommand;
import com.kori.application.command.TransactionHistoryView;
import com.kori.application.port.in.SearchTransactionHistoryUseCase;
import com.kori.application.result.TransactionHistoryResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class SearchTransactionHistoryPayByCardViewClientHappyPathIT {

    @Autowired SearchTransactionHistoryUseCase searchTransactionHistoryUseCase;
    @Autowired TransactionJpaRepository transactionJpaRepository;
    @Autowired JdbcTemplate jdbcTemplate;

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

        // Create transactions via JPA (avoid fragile SQL)
        transactionJpaRepository.save(new TransactionEntity(
                tx1,
                TransactionType.PAY_BY_CARD.name(),
                new BigDecimal("100.00"),
                t1,
                null
        ));
        transactionJpaRepository.save(new TransactionEntity(
                tx2,
                TransactionType.PAY_BY_CARD.name(),
                new BigDecimal("50.00"),
                t2,
                null
        ));
        transactionJpaRepository.save(new TransactionEntity(
                txOther,
                TransactionType.PAY_BY_CARD.name(),
                new BigDecimal("999.00"),
                t2,
                null
        ));
        transactionJpaRepository.flush();

        // Ledger for tx1: client debited 102.00, merchant credited 100.00, platform credited 2.00
        insertLedger(tx1, "CLIENT", "DEBIT", "102.00", clientId, t1);
        insertLedger(tx1, "MERCHANT", "CREDIT", "100.00", merchantId, t1);
        insertLedger(tx1, "PLATFORM", "CREDIT", "2.00", null, t1);

        // Ledger for tx2: client debited 51.00, merchant credited 50.00, platform credited 1.00
        insertLedger(tx2, "CLIENT", "DEBIT", "51.00", clientId, t2);
        insertLedger(tx2, "MERCHANT", "CREDIT", "50.00", merchantId, t2);
        insertLedger(tx2, "PLATFORM", "CREDIT", "1.00", null, t2);

        // Noise: another client's tx should NOT be visible
        insertLedger(txOther, "CLIENT", "DEBIT", "999.00", otherClientId, t2);
        insertLedger(txOther, "MERCHANT", "CREDIT", "990.00", merchantId, t2);
        insertLedger(txOther, "PLATFORM", "CREDIT", "9.00", null, t2);

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

    private void insertLedger(UUID txId,
                              String account,
                              String entryType,
                              String amount,
                              String referenceId,
                              OffsetDateTime createdAt) {
        jdbcTemplate.update(
                "insert into ledger_entries (id, transaction_id, account, entry_type, amount, reference_id, created_at) " +
                        "values (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(),
                txId,
                account,
                entryType,
                new BigDecimal(amount),
                referenceId,
                createdAt
        );
    }
}
