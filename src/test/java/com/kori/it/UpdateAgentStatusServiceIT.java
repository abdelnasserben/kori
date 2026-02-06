package com.kori.it;

import com.kori.application.command.UpdateAgentStatusCommand;
import com.kori.application.exception.BalanceMustBeZeroException;
import com.kori.application.port.in.UpdateAgentStatusUseCase;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.common.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

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

        LedgerAccountRef agentWallet = LedgerAccountRef.agentWallet(agent.id().value().toString());
        LedgerAccountRef agentClearing = LedgerAccountRef.agentCashClearing(agent.id().value().toString());
        AccountProfile walletProfile = accountProfilePort.findByAccount(agentWallet).orElseThrow();
        AccountProfile clearingProfile = accountProfilePort.findByAccount(agentClearing).orElseThrow();
        assertEquals(Status.SUSPENDED, walletProfile.status());
        assertEquals(Status.SUSPENDED, clearingProfile.status());

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().contains("ADMIN_UPDATE_AGENT_STATUS"))
        );
    }

    @Test
    void closeAgent_refused_whenAgentWalletBalanceIsNotZero() {
        Agent agent = createActiveAgent("A-343434");
        seedLedgerCredit(LedgerAccountRef.agentWallet(agent.id().value().toString()), java.math.BigDecimal.TEN);

        assertThrows(BalanceMustBeZeroException.class, () ->
                updateAgentStatusUseCase.execute(new UpdateAgentStatusCommand(
                        adminActor(),
                        agent.code().value(),
                        Status.CLOSED.name(),
                        "test"
                ))
        );

        Agent updated = agentRepositoryPort.findByCode(agent.code()).orElseThrow();
        assertEquals(Status.ACTIVE, updated.status());

        LedgerAccountRef agentWallet = LedgerAccountRef.agentWallet(agent.id().value().toString());
        LedgerAccountRef agentClearing = LedgerAccountRef.agentCashClearing(agent.id().value().toString());
        AccountProfile walletProfile = accountProfilePort.findByAccount(agentWallet).orElseThrow();
        AccountProfile clearingProfile = accountProfilePort.findByAccount(agentClearing).orElseThrow();
        assertEquals(Status.ACTIVE, walletProfile.status());
        assertEquals(Status.ACTIVE, clearingProfile.status());
    }
    @Test
    void closeAgent_refused_whenAgentCashClearingBalanceIsNotZero() {
        Agent agent = createActiveAgent("A-565656");
        seedLedgerCredit(LedgerAccountRef.agentCashClearing(agent.id().value().toString()), java.math.BigDecimal.TEN);

        assertThrows(BalanceMustBeZeroException.class, () ->
                updateAgentStatusUseCase.execute(new UpdateAgentStatusCommand(
                        adminActor(),
                        agent.code().value(),
                        Status.CLOSED.name(),
                        "test"
                ))
        );

        Agent updated = agentRepositoryPort.findByCode(agent.code()).orElseThrow();
        assertEquals(Status.ACTIVE, updated.status());

        LedgerAccountRef agentWallet = LedgerAccountRef.agentWallet(agent.id().value().toString());
        LedgerAccountRef agentClearing = LedgerAccountRef.agentCashClearing(agent.id().value().toString());
        AccountProfile walletProfile = accountProfilePort.findByAccount(agentWallet).orElseThrow();
        AccountProfile clearingProfile = accountProfilePort.findByAccount(agentClearing).orElseThrow();
        assertEquals(Status.ACTIVE, walletProfile.status());
        assertEquals(Status.ACTIVE, clearingProfile.status());
    }
}
