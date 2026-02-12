package com.kori.it;

import com.kori.application.command.AdminUnblockCardCommand;
import com.kori.application.port.in.AdminUnblockCardUseCase;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardStatus;
import com.kori.domain.model.client.Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminUnblockCardServiceIT extends IntegrationTestBase {

    @Autowired
    AdminUnblockCardUseCase adminUnblockCardUseCase;

    @Test
    void adminUnblockCard_unblocksCardAndWritesAudit() {
        Client client = createActiveClient("+262700000333");
        String cardUid = "04A1B2C3D4E5F6A7B8C9D";
        createCardWithStatus(client, cardUid, "1234", CardStatus.BLOCKED, 2);

        adminUnblockCardUseCase.execute(new AdminUnblockCardCommand(
                adminActor(),
                cardUid,
                "test"
        ));

        Card updated = cardRepositoryPort.findByCardUid(cardUid).orElseThrow();
        assertEquals(CardStatus.ACTIVE, updated.status());
        assertEquals(0, updated.failedPinAttempts());

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().contains("ADMIN_UNBLOCK_CARD"))
        );
    }
}
