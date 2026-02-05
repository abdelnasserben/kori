package com.kori.it;

import com.kori.application.command.ReversalCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.ReversalUseCase;
import com.kori.application.result.ReversalResult;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;
import com.kori.domain.model.transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReversalPolicyIT extends IntegrationTestBase {

    @Autowired
    ReversalUseCase reversalUseCase;

    @Test
    void digitalPayByCard_refundableFlagControlsFeeRefund() {
        TransactionId originalId = new TransactionId(UUID.randomUUID());
        Transaction original = new Transaction(originalId, TransactionType.PAY_BY_CARD, Money.of(new BigDecimal("50.00")), NOW, null);
        transactionRepositoryPort.save(original);

        LedgerAccountRef client = LedgerAccountRef.client("C1");
        LedgerAccountRef merchant = LedgerAccountRef.merchant("M1");
        LedgerAccountRef fee = LedgerAccountRef.platformFeeRevenue();

        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(original.id(), client, Money.of(new BigDecimal("52.00"))),
                LedgerEntry.credit(original.id(), merchant, Money.of(new BigDecimal("50.00"))),
                LedgerEntry.credit(original.id(), fee, Money.of(new BigDecimal("2.00")))
        ));

        jdbcTemplate.update("UPDATE fee_config SET card_payment_fee_refundable = false WHERE id = 1");
        ReversalResult first = reversalUseCase.execute(new ReversalCommand("idem-s4-d1", "h1", adminActor(), original.id().value().toString()));
        UUID firstRevId = UUID.fromString(first.reversalTransactionId());

        Integer feeDebitCountFalse = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ledger_entries WHERE transaction_id = ? AND entry_type='DEBIT' AND account_type='PLATFORM_FEE_REVENUE'",
                Integer.class,
                firstRevId
        );
        assertEquals(0, feeDebitCountFalse);

        TransactionId originalId2 = new TransactionId(UUID.randomUUID());
        transactionRepositoryPort.save(new Transaction(originalId2, TransactionType.PAY_BY_CARD, Money.of(new BigDecimal("50.00")), NOW, null));
        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(originalId2, client, Money.of(new BigDecimal("52.00"))),
                LedgerEntry.credit(originalId2, merchant, Money.of(new BigDecimal("50.00"))),
                LedgerEntry.credit(originalId2, fee, Money.of(new BigDecimal("2.00")))
        ));

        jdbcTemplate.update("UPDATE fee_config SET card_payment_fee_refundable = true WHERE id = 1");
        ReversalResult second = reversalUseCase.execute(new ReversalCommand("idem-s4-d2", "h2", adminActor(), originalId2.value().toString()));
        UUID secondRevId = UUID.fromString(second.reversalTransactionId());

        Integer feeDebitCountTrue = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ledger_entries WHERE transaction_id = ? AND entry_type='DEBIT' AND account_type='PLATFORM_FEE_REVENUE'",
                Integer.class,
                secondRevId
        );
        assertEquals(1, feeDebitCountTrue);
    }

    @Test
    void cashReversalBlocked_whenCashLimitWouldBeViolated() {
        jdbcTemplate.update("UPDATE platform_config SET agent_cash_limit_global = 90.00 WHERE id = 1");

        TransactionId originalId = new TransactionId(UUID.randomUUID());
        transactionRepositoryPort.save(new Transaction(originalId, TransactionType.MERCHANT_WITHDRAW_AT_AGENT, Money.of(new BigDecimal("100.00")), NOW, null));

        LedgerAccountRef merchant = LedgerAccountRef.merchant("M2");
        LedgerAccountRef clearing = LedgerAccountRef.agentCashClearing("A2");
        LedgerAccountRef fee = LedgerAccountRef.platformFeeRevenue();

        TransactionId priorCashOutId = new TransactionId(UUID.randomUUID());
        transactionRepositoryPort.save(new Transaction(priorCashOutId, TransactionType.CASH_IN_BY_AGENT, Money.of(new BigDecimal("200.00")), NOW, null));
        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(priorCashOutId, clearing, Money.of(new BigDecimal("200.00"))),
                LedgerEntry.credit(priorCashOutId, LedgerAccountRef.client("C-cash-block"), Money.of(new BigDecimal("200.00")))
        ));

        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(originalId, merchant, Money.of(new BigDecimal("103.00"))),
                LedgerEntry.credit(originalId, clearing, Money.of(new BigDecimal("100.00"))),
                LedgerEntry.credit(originalId, fee, Money.of(new BigDecimal("3.00")))
        ));

        ForbiddenOperationException ex = assertThrows(ForbiddenOperationException.class, () ->
                reversalUseCase.execute(new ReversalCommand("idem-s4-c1", "hc1", adminActor(), originalId.value().toString()))
        );
        assertTrue(ex.getMessage().contains("Agent cash limit exceeded"));

        Integer totalEntries = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ledger_entries", Integer.class);
        assertEquals(5, totalEntries);
        assertFalse(transactionRepositoryPort.existsReversalFor(originalId));
    }

    @Test
    void cashReversalAllowed_whenProjectedBalanceWithinLimit_andBalanced() {
        jdbcTemplate.update("UPDATE platform_config SET agent_cash_limit_global = 1000.00 WHERE id = 1");
        jdbcTemplate.update("UPDATE fee_config SET merchant_withdraw_fee_refundable = true WHERE id = 1");

        TransactionId originalId = new TransactionId(UUID.randomUUID());
        transactionRepositoryPort.save(new Transaction(originalId, TransactionType.MERCHANT_WITHDRAW_AT_AGENT, Money.of(new BigDecimal("100.00")), NOW, null));

        LedgerAccountRef merchant = LedgerAccountRef.merchant("M3");
        LedgerAccountRef clearing = LedgerAccountRef.agentCashClearing("A3");
        LedgerAccountRef fee = LedgerAccountRef.platformFeeRevenue();

        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(originalId, merchant, Money.of(new BigDecimal("103.00"))),
                LedgerEntry.credit(originalId, clearing, Money.of(new BigDecimal("100.00"))),
                LedgerEntry.credit(originalId, fee, Money.of(new BigDecimal("3.00")))
        ));

        ReversalResult result = reversalUseCase.execute(new ReversalCommand("idem-s4-c2", "hc2", adminActor(), originalId.value().toString()));
        UUID revId = UUID.fromString(result.reversalTransactionId());

        assertTrue(transactionRepositoryPort.existsReversalFor(originalId));

        BigDecimal debits = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount),0) FROM ledger_entries WHERE transaction_id = ? AND entry_type='DEBIT'",
                BigDecimal.class,
                revId
        );
        BigDecimal credits = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount),0) FROM ledger_entries WHERE transaction_id = ? AND entry_type='CREDIT'",
                BigDecimal.class,
                revId
        );
        assertEquals(0, debits.compareTo(credits));
    }

    @Test
    void reversalIsIdempotent_noSecondLedgerWrite() {
        TransactionId originalId = new TransactionId(UUID.randomUUID());
        transactionRepositoryPort.save(new Transaction(originalId, TransactionType.PAY_BY_CARD, Money.of(new BigDecimal("20.00")), NOW, null));

        LedgerAccountRef client = LedgerAccountRef.client("C4");
        LedgerAccountRef merchant = LedgerAccountRef.merchant("M4");
        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(originalId, client, Money.of(new BigDecimal("20.00"))),
                LedgerEntry.credit(originalId, merchant, Money.of(new BigDecimal("20.00")))
        ));

        reversalUseCase.execute(new ReversalCommand("idem-s4-i1", "hi1", adminActor(), originalId.value().toString()));

        Integer afterFirst = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ledger_entries", Integer.class);
        assertNotNull(afterFirst);

        ForbiddenOperationException ex = assertThrows(ForbiddenOperationException.class, () ->
                reversalUseCase.execute(new ReversalCommand("idem-s4-i2", "hi2", adminActor(), originalId.value().toString()))
        );
        assertTrue(ex.getMessage().contains("already reversed"));

        Integer afterSecond = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ledger_entries", Integer.class);
        assertEquals(afterFirst, afterSecond);
    }
}
