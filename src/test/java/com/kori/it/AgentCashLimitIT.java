package com.kori.it;

import com.kori.application.command.CashInByAgentCommand;
import com.kori.application.command.EnrollCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.CashInByAgentUseCase;
import com.kori.application.port.in.EnrollCardUseCase;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.client.Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AgentCashLimitIT extends IntegrationTestBase {

    @Autowired
    private CashInByAgentUseCase cashInByAgentUseCase;

    @Autowired
    private EnrollCardUseCase enrollCardUseCase;

    @Test
    void withZeroGlobalLimit_enrollAndCashInAreRejected_andNoLedgerEntryWritten() {
        jdbcTemplate.update("UPDATE platform_config SET agent_cash_limit_global = 0.00 WHERE id = 1");

        Agent agent = createActiveAgent("A-999001");
        Client client = createActiveClient("+2691111000");

        ForbiddenOperationException enrollEx = assertThrows(ForbiddenOperationException.class, () ->
                enrollCardUseCase.execute(new EnrollCardCommand(
                        "idem-limit-enroll",
                        "hash-limit-enroll",
                        agentActor(agent.id().value().toString()),
                        "+2691111999",
                        "CARD-LIMIT-001",
                        "1234",
                        agent.code().value()
                ))
        );
        assertTrue(enrollEx.getMessage().contains("Agent cash limit exceeded"));

        ForbiddenOperationException cashInEx = assertThrows(ForbiddenOperationException.class, () ->
                cashInByAgentUseCase.execute(new CashInByAgentCommand(
                        "idem-limit-cashin",
                        "hash-limit-cashin",
                        agentActor(agent.id().value().toString()),
                        client.phoneNumber().value(),
                        new BigDecimal("100.00")
                ))
        );
        assertTrue(cashInEx.getMessage().contains("Agent cash limit exceeded"));

        Integer ledgerCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ledger_entries", Integer.class);
        assertEquals(0, ledgerCount);
    }

    @Test
    void withLimit1000_first500Passes_then600Rejected_andNoEntryWrittenForRejectedTx() {
        jdbcTemplate.update("UPDATE platform_config SET agent_cash_limit_global = 1000.00 WHERE id = 1");

        Agent agent = createActiveAgent("A-999002");
        Client client = createActiveClient("+2691111001");

        cashInByAgentUseCase.execute(new CashInByAgentCommand(
                "idem-limit-pass",
                "hash-limit-pass",
                agentActor(agent.id().value().toString()),
                client.phoneNumber().value(),
                new BigDecimal("500.00")
        ));

        Integer afterFirst = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ledger_entries", Integer.class);
        assertEquals(2, afterFirst);

        ForbiddenOperationException ex = assertThrows(ForbiddenOperationException.class, () ->
                cashInByAgentUseCase.execute(new CashInByAgentCommand(
                        "idem-limit-fail",
                        "hash-limit-fail",
                        agentActor(agent.id().value().toString()),
                        client.phoneNumber().value(),
                        new BigDecimal("600.00")
                ))
        );
        assertTrue(ex.getMessage().contains("Agent cash limit exceeded"));

        Integer finalCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ledger_entries", Integer.class);
        assertEquals(2, finalCount);
    }
}
