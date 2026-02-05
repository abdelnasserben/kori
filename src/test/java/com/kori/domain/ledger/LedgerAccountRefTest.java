package com.kori.domain.ledger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LedgerAccountRefTest {

    @Test
    void supportsNewAccountTypesAndHelpers() {
        assertEquals(LedgerAccountType.AGENT_WALLET, LedgerAccountRef.agentWallet("agent-1").type());
        assertEquals("agent-1", LedgerAccountRef.agentWallet("agent-1").ownerRef());

        assertEquals(LedgerAccountType.AGENT_CASH_CLEARING, LedgerAccountRef.agentCashClearing("agent-2").type());
        assertEquals("agent-2", LedgerAccountRef.agentCashClearing("agent-2").ownerRef());

        assertEquals(LedgerAccountType.PLATFORM_BANK, LedgerAccountRef.platformBank().type());
        assertEquals("SYSTEM", LedgerAccountRef.platformBank().ownerRef());
    }

    @Test
    void parsesAllNewTypesWithValueOf() {
        assertEquals(LedgerAccountType.AGENT_WALLET, LedgerAccountType.valueOf("AGENT_WALLET"));
        assertEquals(LedgerAccountType.AGENT_CASH_CLEARING, LedgerAccountType.valueOf("AGENT_CASH_CLEARING"));
        assertEquals(LedgerAccountType.PLATFORM_BANK, LedgerAccountType.valueOf("PLATFORM_BANK"));
    }

    @Test
    void newAgentScopesAreRecognizedAsAgentScopes() {
        assertTrue(LedgerAccountRef.agentWallet("a-1").isForAgent());
        assertTrue(LedgerAccountRef.agentCashClearing("a-1").isForAgent());
    }
}
