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
                "request-hash",
                adminActor()
        ));

        assertNotNull(result.agentId());
        assertNotNull(result.agentCode());

        Agent agent = agentRepositoryPort.findByCode(AgentCode.of(result.agentCode())).orElseThrow();
        assertEquals(Status.ACTIVE, agent.status());

        LedgerAccountRef agentWallet = LedgerAccountRef.agentWallet(result.agentId());
        LedgerAccountRef agentClearing = LedgerAccountRef.agentCashClearing(result.agentId());
        AccountProfile walletProfile = accountProfilePort.findByAccount(agentWallet).orElseThrow();
        AccountProfile clearingProfile = accountProfilePort.findByAccount(agentClearing).orElseThrow();
        assertEquals(Status.ACTIVE, walletProfile.status());
        assertEquals(Status.ACTIVE, clearingProfile.status());

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().contains("AGENT_CREATED"))
        );
    }
}
