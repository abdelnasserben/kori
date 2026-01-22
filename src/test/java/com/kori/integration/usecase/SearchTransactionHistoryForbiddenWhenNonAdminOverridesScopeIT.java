package com.kori.integration.usecase;

import com.kori.application.command.SearchTransactionHistoryCommand;
import com.kori.application.command.TransactionHistoryView;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.SearchTransactionHistoryUseCase;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccount;
import com.kori.domain.model.transaction.TransactionType;
import com.kori.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchTransactionHistoryForbiddenWhenNonAdminOverridesScopeIT extends AbstractIntegrationTest {

    @Autowired
    SearchTransactionHistoryUseCase searchTransactionHistoryUseCase;

    @Test
    void nonAdmin_cannotProvideLedgerAccountAndReferenceId() {
        // Given
        String clientId = UUID.randomUUID().toString();

        // When / Then: CLIENT tries to override scope to some other reference id
        assertThrows(ForbiddenOperationException.class, () ->
                searchTransactionHistoryUseCase.execute(
                        new SearchTransactionHistoryCommand(
                                new ActorContext(ActorType.CLIENT, clientId, Map.of()),
                                LedgerAccount.MERCHANT,                 // override attempt
                                "MERCHANT_" + UUID.randomUUID(),        // override attempt
                                TransactionType.PAY_BY_CARD,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                TransactionHistoryView.SUMMARY,
                                20
                        )
                )
        );
    }
}
