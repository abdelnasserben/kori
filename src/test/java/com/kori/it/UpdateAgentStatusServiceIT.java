package com.kori.it;

import com.kori.application.command.UpdateAgentStatusCommand;
import com.kori.application.port.in.UpdateAgentStatusUseCase;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.common.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateAgentStatusServiceIT extends IntegrationTestBase {

    private static final String AGENT_CODE = "A-121212";

    @Autowired
    UpdateAgentStatusUseCase updateAgentStatusUseCase;

    @Test
    void updateAgentStatus_suspendsAccountProfile() {
        Agent agent = createActiveAgent(AGENT_CODE);

        updateAgentStatusUseCase.execute(new UpdateAgentStatusCommand(
                adminActor(),
                AGENT_CODE,
                Status.SUSPENDED.name(),
                "test"
        ));

        Agent updated = agentRepositoryPort.findByCode(agent.code()).orElseThrow();
        assertEquals(Status.SUSPENDED, updated.status());

        LedgerAccountRef agentAccount = LedgerAccountRef.agent(agent.id().value().toString());
        AccountProfile profile = accountProfilePort.findByAccount(agentAccount).orElseThrow();
        assertEquals(Status.SUSPENDED, profile.status());

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().contains("ADMIN_UPDATE_AGENT_STATUS"))
        );
    }
}
