package com.kori.it;

import com.kori.application.command.AdminUpdateCardStatusCommand;
import com.kori.application.port.in.AdminUpdateCardStatusUseCase;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardStatus;
import com.kori.domain.model.client.Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminUpdateCardStatusServiceIT extends IntegrationTestBase {

    @Autowired
    AdminUpdateCardStatusUseCase adminUpdateCardStatusUseCase;

    @Test
    void adminUpdateCardStatus_suspendsCardAndWritesAudit() {
        Client client = createActiveClient("+262700000111");
        UUID cardUid = UUID.randomUUID();
        createActiveCard(client, cardUid.toString(), "1234");

        adminUpdateCardStatusUseCase.execute(new AdminUpdateCardStatusCommand(
                adminActor(),
                cardUid,
                CardStatus.SUSPENDED.name(),
                "test"
        ));

        Card updated = cardRepositoryPort.findByCardUid(cardUid.toString()).orElseThrow();
        assertEquals(CardStatus.SUSPENDED, updated.status());

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().contains("ADMIN_CARD_STATUS_UPDATED_SUSPENDED"))
        );
    }
}
