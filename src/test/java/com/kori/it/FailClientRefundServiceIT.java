package com.kori.it;

import com.kori.application.command.FailClientRefundCommand;
import com.kori.application.command.RequestClientRefundCommand;
import com.kori.application.port.in.FailClientRefundUseCase;
import com.kori.application.port.in.RequestClientRefundUseCase;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.common.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FailClientRefundServiceIT extends IntegrationTestBase {
    @Autowired RequestClientRefundUseCase requestClientRefundUseCase;
    @Autowired FailClientRefundUseCase failClientRefundUseCase;

    @Test
    void failClientRefund_returns_funds_to_client_wallet() {
        Client client = createActiveClient("7712347");
        var clientAcc = LedgerAccountRef.client(client.id().value().toString());
        seedLedgerCredit(clientAcc, new BigDecimal("35.00"));

        var requested = requestClientRefundUseCase.execute(new RequestClientRefundCommand("idem-refund-3", "hash", adminActor(), client.id().value().toString()));
        failClientRefundUseCase.execute(new FailClientRefundCommand(adminActor(), requested.refundId(), "bank reject"));

        assertEquals(Money.of(new BigDecimal("35.00")), ledgerQueryPort.netBalance(clientAcc));
        assertEquals(Money.zero(), ledgerQueryPort.netBalance(LedgerAccountRef.platformClientRefundClearing()));
    }

    @Test
    void failClientRefund_isIdempotent_whenAlreadyFailed() {
        Client client = createActiveClient("7712349");
        var clientAcc = LedgerAccountRef.client(client.id().value().toString());
        seedLedgerCredit(clientAcc, new BigDecimal("35.00"));

        var requested = requestClientRefundUseCase.execute(new RequestClientRefundCommand("idem-refund-3-bis", "hash", adminActor(), client.id().value().toString()));
        failClientRefundUseCase.execute(new FailClientRefundCommand(adminActor(), requested.refundId(), "bank reject"));

        int ledgerCountAfterFirstCall = ledgerQueryPort.findByTransactionId(com.kori.domain.model.transaction.TransactionId.of(requested.transactionId())).size();
        long failedAuditAfterFirstCall = auditEventJpaRepository.findAll().stream()
                .filter(event -> event.getAction().equals("CLIENT_REFUND_FAILED"))
                .count();

        failClientRefundUseCase.execute(new FailClientRefundCommand(adminActor(), requested.refundId(), "bank reject"));

        assertEquals(ledgerCountAfterFirstCall, ledgerQueryPort.findByTransactionId(com.kori.domain.model.transaction.TransactionId.of(requested.transactionId())).size());
        assertEquals(failedAuditAfterFirstCall, auditEventJpaRepository.findAll().stream()
                .filter(event -> event.getAction().equals("CLIENT_REFUND_FAILED"))
                .count());
    }
}
