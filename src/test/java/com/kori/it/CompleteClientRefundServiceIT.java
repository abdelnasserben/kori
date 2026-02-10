package com.kori.it;

import com.kori.application.command.CompleteClientRefundCommand;
import com.kori.application.command.RequestClientRefundCommand;
import com.kori.application.port.in.CompleteClientRefundUseCase;
import com.kori.application.port.in.RequestClientRefundUseCase;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.common.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompleteClientRefundServiceIT extends IntegrationTestBase {
    @Autowired RequestClientRefundUseCase requestClientRefundUseCase;
    @Autowired CompleteClientRefundUseCase completeClientRefundUseCase;

    @Test
    void completeClientRefund_moves_to_platform_bank() {
        Client client = createActiveClient("7712346");
        var clientAcc = LedgerAccountRef.client(client.id().value().toString());
        seedLedgerCredit(clientAcc, new BigDecimal("30.00"));

        var requested = requestClientRefundUseCase.execute(new RequestClientRefundCommand("idem-refund-2", "hash", adminActor(), client.id().value().toString()));
        completeClientRefundUseCase.execute(new CompleteClientRefundCommand(adminActor(), requested.refundId()));

        assertEquals(Money.zero(), ledgerQueryPort.netBalance(LedgerAccountRef.platformClientRefundClearing()));
        assertEquals(Money.of(new BigDecimal("30.00")), ledgerQueryPort.netBalance(LedgerAccountRef.platformBank()));
    }

    @Test
    void completeClientRefund_isIdempotent_whenAlreadyCompleted() {
        Client client = createActiveClient("7712348");
        var clientAcc = LedgerAccountRef.client(client.id().value().toString());
        seedLedgerCredit(clientAcc, new BigDecimal("30.00"));

        var requested = requestClientRefundUseCase.execute(new RequestClientRefundCommand("idem-refund-2-bis", "hash", adminActor(), client.id().value().toString()));
        completeClientRefundUseCase.execute(new CompleteClientRefundCommand(adminActor(), requested.refundId()));

        int ledgerCountAfterFirstCall = ledgerQueryPort.findByTransactionId(com.kori.domain.model.transaction.TransactionId.of(requested.transactionId())).size();
        long completedAuditAfterFirstCall = auditEventJpaRepository.findAll().stream()
                .filter(event -> event.getAction().equals("CLIENT_REFUND_COMPLETED"))
                .count();

        completeClientRefundUseCase.execute(new CompleteClientRefundCommand(adminActor(), requested.refundId()));

        assertEquals(ledgerCountAfterFirstCall, ledgerQueryPort.findByTransactionId(com.kori.domain.model.transaction.TransactionId.of(requested.transactionId())).size());
        assertEquals(completedAuditAfterFirstCall, auditEventJpaRepository.findAll().stream()
                .filter(event -> event.getAction().equals("CLIENT_REFUND_COMPLETED"))
                .count());
    }
}
