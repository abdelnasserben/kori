package com.kori.it;

import com.kori.application.command.AdminUpdateCardStatusCommand;
import com.kori.application.port.in.AdminUpdateCardStatusUseCase;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardStatus;
import com.kori.domain.model.client.Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminUpdateCardStatusServiceIT extends IntegrationTestBase {

    @Autowired
    AdminUpdateCardStatusUseCase adminUpdateCardStatusUseCase;

    @Test
    void adminUpdateCardStatus_suspendsCardAndWritesAudit() {
        Client client = createActiveClient("+262700000111");
        String cardUid = "04A1B2C3D4E5F6A7B8C9D";
        createActiveCard(client, cardUid, "1234");

        adminUpdateCardStatusUseCase.execute(new AdminUpdateCardStatusCommand(
                adminActor(),
                cardUid,
                CardStatus.SUSPENDED.name(),
                "test"
        ));

        Card updated = cardRepositoryPort.findByCardUid(cardUid).orElseThrow();
        assertEquals(CardStatus.SUSPENDED, updated.status());

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().contains("ADMIN_CARD_STATUS_UPDATED_SUSPENDED"))
        );
    }
}
