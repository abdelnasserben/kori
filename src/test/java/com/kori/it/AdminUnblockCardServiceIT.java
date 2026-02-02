package com.kori.it;

import com.kori.application.command.AdminUnblockCardCommand;
import com.kori.application.port.in.AdminUnblockCardUseCase;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardStatus;
import com.kori.domain.model.client.Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminUnblockCardServiceIT extends IntegrationTestBase {

    @Autowired
    AdminUnblockCardUseCase adminUnblockCardUseCase;

    @Test
    void adminUnblockCard_unblocksCardAndWritesAudit() {
        Client client = createActiveClient("+262700000333");
        UUID cardUid = UUID.randomUUID();
        createCardWithStatus(client, cardUid.toString(), "1234", CardStatus.BLOCKED, 2);

        adminUnblockCardUseCase.execute(new AdminUnblockCardCommand(
                adminActor(),
                cardUid,
                "test"
        ));

        Card updated = cardRepositoryPort.findByCardUid(cardUid.toString()).orElseThrow();
        assertEquals(CardStatus.ACTIVE, updated.status());
        assertEquals(0, updated.failedPinAttempts());

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().contains("ADMIN_UNBLOCK_CARD"))
        );
    }
}
