package com.kori.it;

import com.kori.application.command.UpdateClientStatusCommand;
import com.kori.application.exception.BalanceMustBeZeroException;
import com.kori.application.port.in.UpdateClientStatusUseCase;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardStatus;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.common.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class UpdateClientStatusServiceIT extends IntegrationTestBase {

    @Autowired
    UpdateClientStatusUseCase updateClientStatusUseCase;

    @Test
    void updateClientStatus_suspendsAccountProfileAndCards() {
        Client client = createActiveClient("+262700000000");
        Card card = createActiveCard(client, "CARD-CLIENT-001", "1234");

        updateClientStatusUseCase.execute(new UpdateClientStatusCommand(
                adminActor(),
                client.id().value().toString(),
                Status.SUSPENDED.name(),
                "test"
        ));

        Client updated = clientRepositoryPort.findById(client.id()).orElseThrow();
        assertEquals(Status.SUSPENDED, updated.status());

        LedgerAccountRef clientAccount = LedgerAccountRef.client(client.id().value().toString());
        AccountProfile profile = accountProfilePort.findByAccount(clientAccount).orElseThrow();
        assertEquals(Status.SUSPENDED, profile.status());

        Card updatedCard = cardRepositoryPort.findByCardUid(card.cardUid()).orElseThrow();
        assertEquals(CardStatus.SUSPENDED, updatedCard.status());

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().contains("ADMIN_UPDATE_CLIENT_STATUS"))
        );
    }

    @Test
    void closeClient_refused_whenClientWalletBalanceIsNotZero() {
        Client client = createActiveClient("+262700000333");
        Card card = createActiveCard(client, "CARD-CLIENT-333", "1234");
        seedLedgerCredit(LedgerAccountRef.client(client.id().value().toString()), java.math.BigDecimal.TEN);

        assertThrows(BalanceMustBeZeroException.class, () ->
                updateClientStatusUseCase.execute(new UpdateClientStatusCommand(
                        adminActor(),
                        client.id().value().toString(),
                        Status.CLOSED.name(),
                        "test"
                ))
        );

        Client updated = clientRepositoryPort.findById(client.id()).orElseThrow();
        assertEquals(Status.ACTIVE, updated.status());

        AccountProfile profile = accountProfilePort.findByAccount(
                LedgerAccountRef.client(client.id().value().toString())
        ).orElseThrow();
        assertEquals(Status.ACTIVE, profile.status());

        Card updatedCard = cardRepositoryPort.findByCardUid(card.cardUid()).orElseThrow();
        assertEquals(CardStatus.ACTIVE, updatedCard.status());
    }
}
