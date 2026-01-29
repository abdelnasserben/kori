package com.kori.application.handler;

import com.kori.application.events.AgentStatusChangedEvent;
import com.kori.application.port.out.AccountProfilePort;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.common.Status;

import java.util.Objects;

/**
 * Propagation du changement de statut Agent :
 * - SUSPENDED => suspend AccountProfile(agent)
 * - CLOSED    => close AccountProfile(agent)
 * - ACTIVE    => activate AccountProfile(agent)
 */
public class OnAgentStatusChangedHandler {

    private final AccountProfilePort accountProfilePort;

    public OnAgentStatusChangedHandler(AccountProfilePort accountProfilePort) {
        this.accountProfilePort = Objects.requireNonNull(accountProfilePort);
    }

    public void handle(AgentStatusChangedEvent event) {
        Objects.requireNonNull(event, "event");

        if (event.before() == event.after()) {
            return;
        }

        Status after = event.after();

        LedgerAccountRef agentAccountRef =
                LedgerAccountRef.agent(event.agentId().value().toString());

        AccountProfile agentProfile =
                accountProfilePort.findByAccount(agentAccountRef).orElse(null);

        if (agentProfile == null) {
            return; // rien à propager
        }

        switch (after) {
            case SUSPENDED -> {
                agentProfile.suspend();
                accountProfilePort.save(agentProfile);
            }
            case CLOSED -> {
                agentProfile.close();
                accountProfilePort.save(agentProfile);
            }
            case ACTIVE -> {
                agentProfile.activate();
                accountProfilePort.save(agentProfile);
            }
        }
    }
}
