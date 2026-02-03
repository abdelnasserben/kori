package com.kori.it;

import com.kori.application.command.ReversalCommand;
import com.kori.application.port.in.ReversalUseCase;
import com.kori.application.result.ReversalResult;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.ledger.LedgerEntryType;
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

class ReversalServiceIT extends IntegrationTestBase {

    private static final Money AMOUNT = Money.of(new BigDecimal("20.00"));

    @Autowired
    ReversalUseCase reversalUseCase;

    @Test
    void reversal_happyPath_createsCompensatingEntriesAndAudit() {
        TransactionId originalId = new TransactionId(UUID.randomUUID());
        Transaction original = new Transaction(originalId, TransactionType.PAY_BY_CARD, AMOUNT, NOW, null);
        transactionRepositoryPort.save(original);

        LedgerAccountRef clientAccount = LedgerAccountRef.client(UUID.randomUUID().toString());
        LedgerAccountRef merchantAccount = LedgerAccountRef.merchant(UUID.randomUUID().toString());

        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(original.id(), clientAccount, AMOUNT),
                LedgerEntry.credit(original.id(), merchantAccount, AMOUNT)
        ));

        ReversalResult result = reversalUseCase.execute(new ReversalCommand(
                "idem-reversal-1",
                "request-hash",
                adminActor(),
                original.id().value().toString()
        ));

        // Reversal executed
        assertNotNull(result.originalTransactionId());
        assertTrue(auditEventJpaRepository.findAll().stream()
                        .anyMatch(event -> event.getAction().equals("REVERSAL")));


        // Client is credited
        List<LedgerEntry> clientEntries = ledgerQueryPort.findEntries(clientAccount);
        assertEquals(2, clientEntries.size());

        assertTrue(clientEntries.stream().anyMatch(entry ->
                entry.type() == LedgerEntryType.CREDIT
                        && entry.accountRef().equals(clientAccount)
                        && entry.amount().equals(AMOUNT)
        ));

        // Merchant is debited
        List<LedgerEntry> merchantEntries = ledgerQueryPort.findEntries(clientAccount);
        assertEquals(2, merchantEntries.size());

        assertTrue(merchantEntries.stream().anyMatch(entry ->
                entry.type() == LedgerEntryType.DEBIT
                        && entry.accountRef().equals(clientAccount)
                        && entry.amount().equals(AMOUNT)
        ));
    }
}
