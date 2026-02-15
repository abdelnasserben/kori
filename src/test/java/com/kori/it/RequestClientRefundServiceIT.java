package com.kori.it;

import com.kori.application.command.RequestClientRefundCommand;
import com.kori.application.port.in.RequestClientRefundUseCase;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.TransactionId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestClientRefundServiceIT extends IntegrationTestBase {

    @Autowired RequestClientRefundUseCase requestClientRefundUseCase;

    @Test
    void requestClientRefund_happyPath() {
        Client client = createActiveClient("+2694631523");
        LedgerAccountRef clientAcc = LedgerAccountRef.client(client.id().value().toString());
        seedLedgerCredit(clientAcc, new BigDecimal("25.00"));

        var result = requestClientRefundUseCase.execute(new RequestClientRefundCommand("idem-refund-1", "hash", adminActor(), client.id().value().toString()));

        var entries = ledgerQueryPort.findByTransactionId(TransactionId.of(result.transactionId()));
        assertEquals(2, entries.size());
        assertEquals(Money.zero(), ledgerQueryPort.netBalance(clientAcc));
        assertEquals(Money.of(new BigDecimal("25.00")), ledgerQueryPort.netBalance(LedgerAccountRef.platformClientRefundClearing()));
    }
}
