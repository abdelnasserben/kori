package com.kori.it;

import com.kori.application.command.PayByCardCommand;
import com.kori.application.exception.InsufficientFundsException;
import com.kori.application.port.in.PayByCardUseCase;
import com.kori.application.result.PayByCardResult;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.ledger.LedgerEntryType;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.terminal.Terminal;
import com.kori.domain.model.transaction.TransactionId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PayByCardServiceIT extends IntegrationTestBase {

    private static final String MERCHANT_CODE = "M-654321";
    private static final String CLIENT_PHONE = "+262699998888";
    private static final String CARD_UID = "CARD-PAY-001";
    private static final String PIN = "1234";

    @Autowired PayByCardUseCase payByCardUseCase;

    @Test
    void payByCard_happyPath_persistsTransactionLedgerAndAudit() {
        Merchant merchant = createActiveMerchant(MERCHANT_CODE);
        Terminal terminal = createActiveTerminal(merchant);

        Client client = createActiveClient(CLIENT_PHONE);
        createActiveCard(client, CARD_UID, PIN);

        seedLedgerCredit(LedgerAccountRef.client(client.id().value().toString()), new BigDecimal("100.00"));

        PayByCardResult result = payByCardUseCase.execute(new PayByCardCommand(
                "idem-pay-1",
                "request-hash",
                terminalActor("terminal-actor"),
                terminal.id().value().toString(),
                CARD_UID,
                PIN,
                new BigDecimal("50.00")
        ));

        assertNotNull(result.transactionId());
        assertEquals(MERCHANT_CODE, result.merchantCode());

        List<LedgerEntry> entries = ledgerQueryPort.findByTransactionId(TransactionId.of(result.transactionId()));
        assertEquals(3, entries.size());

        LedgerAccountRef clientAccount = LedgerAccountRef.client(client.id().value().toString());
        LedgerAccountRef merchantAccount = LedgerAccountRef.merchant(merchant.id().value().toString());

        assertTrue(entries.stream().anyMatch(entry ->
                entry.type() == LedgerEntryType.DEBIT
                        && entry.accountRef().equals(clientAccount)
                        && entry.amount().equals(Money.of(new BigDecimal("51.00")))
        ));

        assertTrue(entries.stream().anyMatch(entry ->
                entry.type() == LedgerEntryType.CREDIT
                        && entry.accountRef().equals(merchantAccount)
                        && entry.amount().equals(Money.of(new BigDecimal("50.00")))
        ));

        assertTrue(entries.stream().anyMatch(entry ->
                entry.type() == LedgerEntryType.CREDIT
                        && entry.accountRef().equals(LedgerAccountRef.platformFeeRevenue())
                        && entry.amount().equals(Money.of(new BigDecimal("1.00")))
        ));

        assertEquals(
                Money.of(new BigDecimal("49.00")),
                ledgerQueryPort.netBalance(clientAccount)
        );

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().equals("PAY_BY_CARD"))
        );
    }

    @Test
    void payByCard_concurrentDoubleSpend_allowsSingleDebit() throws InterruptedException {
        Merchant merchant = createActiveMerchant(MERCHANT_CODE);
        Terminal terminal = createActiveTerminal(merchant);

        Client client = createActiveClient(CLIENT_PHONE);
        createActiveCard(client, CARD_UID, PIN);

        LedgerAccountRef clientAccount = LedgerAccountRef.client(client.id().value().toString());
        seedLedgerCredit(clientAccount, new BigDecimal("51.00"));

        PayByCardCommand cmd1 = new PayByCardCommand(
                "idem-pay-concurrent-1",
                "request-hash-1",
                terminalActor("terminal-actor"),
                terminal.id().value().toString(),
                CARD_UID,
                PIN,
                new BigDecimal("50.00")
        );
        PayByCardCommand cmd2 = new PayByCardCommand(
                "idem-pay-concurrent-2",
                "request-hash-2",
                terminalActor("terminal-actor"),
                terminal.id().value().toString(),
                CARD_UID,
                PIN,
                new BigDecimal("50.00")
        );

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger insufficient = new AtomicInteger();
        AtomicReference<Throwable> unexpected = new AtomicReference<>();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            executor.execute(() -> runConcurrentPayByCard(cmd1, ready, start, done, successes, insufficient, unexpected));
            executor.execute(() -> runConcurrentPayByCard(cmd2, ready, start, done, successes, insufficient, unexpected));

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertTrue(done.await(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        if (unexpected.get() != null) {
            fail("Unexpected exception: " + unexpected.get().getMessage());
        }

        assertEquals(1, successes.get());
        assertEquals(1, insufficient.get());
        assertEquals(Money.zero(), ledgerQueryPort.netBalance(clientAccount));
    }

    private void runConcurrentPayByCard(
            PayByCardCommand command,
            CountDownLatch ready,
            CountDownLatch start,
            CountDownLatch done,
            AtomicInteger successes,
            AtomicInteger insufficient,
            AtomicReference<Throwable> unexpected
    ) {
        try {
            ready.countDown();
            start.await();
            payByCardUseCase.execute(command);
            successes.incrementAndGet();
        } catch (InsufficientFundsException ex) {
            insufficient.incrementAndGet();
        } catch (Exception ex) {
            unexpected.compareAndSet(null, ex);
        } finally {
            done.countDown();
        }
    }
}
