package com.kori.it;

import com.kori.application.command.CreateAgentCommand;
import com.kori.application.port.in.CreateAgentUseCase;
import com.kori.application.result.CreateAgentResult;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.common.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class CreateAgentServiceIT extends IntegrationTestBase {

    @Autowired
    CreateAgentUseCase createAgentUseCase;

    @Test
    void createAgent_happyPath_persistsAgentProfileAndAudit() {
        CreateAgentResult result = createAgentUseCase.execute(new CreateAgentCommand(
                "idem-create-agent-1",
                adminActor()
        ));

        assertNotNull(result.agentId());
        assertNotNull(result.agentCode());

        Agent agent = agentRepositoryPort.findByCode(AgentCode.of(result.agentCode())).orElseThrow();
        assertEquals(Status.ACTIVE, agent.status());

        LedgerAccountRef agentAccount = LedgerAccountRef.agent(result.agentId());
        AccountProfile profile = accountProfilePort.findByAccount(agentAccount).orElseThrow();
        assertEquals(Status.ACTIVE, profile.status());

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().contains("AGENT_CREATED"))
        );
    }
}
