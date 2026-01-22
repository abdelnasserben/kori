package com.kori.integration.usecase;

import com.kori.application.command.ReversalCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.ReversalUseCase;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReversalForbiddenWhenOriginalTransactionNotFoundIT extends AbstractIntegrationTest {

    @Autowired ReversalUseCase reversalUseCase;

    @Test
    void reversal_isForbidden_whenOriginalTransactionDoesNotExist() {
        // Given
        String nonExistingOriginalTxId = UUID.randomUUID().toString();

        long txBefore = transactionJpaRepository.count();
        long ledgerBefore = ledgerEntryJpaRepository.count();
        long auditBefore = auditEventJpaRepository.count();
        long idemBefore = idempotencyJpaRepository.count();

        // When / Then
        assertThrows(ForbiddenOperationException.class, () ->
                reversalUseCase.execute(new ReversalCommand(
                        idemKey("it-reversal-original-not-found"),
                        new ActorContext(ActorType.ADMIN, "admin-actor-it", Map.of()),
                        nonExistingOriginalTxId
                ))
        );

        // And: no side effects
        assertEquals(txBefore, transactionJpaRepository.count(), "No reversal transaction should be created");
        assertEquals(ledgerBefore, ledgerEntryJpaRepository.count(), "No ledger entries should be created");
        assertEquals(auditBefore, auditEventJpaRepository.count(), "No audit event should be created");
        assertEquals(idemBefore, idempotencyJpaRepository.count(), "No idempotency record should be created");
    }
}
