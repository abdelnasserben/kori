package com.kori.it;

import com.kori.application.command.PayByCardCommand;
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
}
