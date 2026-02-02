package com.kori.it;

import com.kori.application.command.AgentUpdateCardStatusCommand;
import com.kori.application.port.in.AgentUpdateCardStatusUseCase;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardStatus;
import com.kori.domain.model.client.Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentUpdateCardStatusServiceIT extends IntegrationTestBase {

    private static final String AGENT_CODE = "A-909090";

    @Autowired
    AgentUpdateCardStatusUseCase agentUpdateCardStatusUseCase;

    @Test
    void agentUpdateCardStatus_marksCardLostAndWritesAudit() {
        Agent agent = createActiveAgent(AGENT_CODE);
        Client client = createActiveClient("+262700000222");
        UUID cardUid = UUID.randomUUID();
        createActiveCard(client, cardUid.toString(), "1234");

        agentUpdateCardStatusUseCase.execute(new AgentUpdateCardStatusCommand(
                agentActor("agent-actor"),
                cardUid,
                agent.code().value(),
                CardStatus.LOST.name(),
                "test"
        ));

        Card updated = cardRepositoryPort.findByCardUid(cardUid.toString()).orElseThrow();
        assertEquals(CardStatus.LOST, updated.status());

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().contains("AGENT_MARK_CARD_LOST"))
        );
    }
}
