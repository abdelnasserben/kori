package com.kori.application.handler;

import com.kori.application.events.AgentStatusChangedEvent;
import com.kori.application.port.out.AccountProfilePort;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.common.Status;

import java.util.List;
import java.util.Objects;

/**
 * Propagation du changement de statut Agent :
 * - SUSPENDED => suspend AccountProfile(agent wallet + cash clearing)
 * - CLOSED    => close AccountProfile(agent wallet + cash clearing)
 * - ACTIVE    => activate AccountProfile(agent wallet + cash clearing)
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

        String agentId = event.agentId().value().toString();

        for (LedgerAccountRef accountRef : List.of(
                LedgerAccountRef.agentWallet(agentId),
                LedgerAccountRef.agentCashClearing(agentId)
        )) {
            accountProfilePort.findByAccount(accountRef)
                    .ifPresent(profile -> applyStatus(after, profile));
        }
    }

    private void applyStatus(Status after, AccountProfile profile) {

        switch (after) {
            case SUSPENDED -> {
                profile.suspend();
                accountProfilePort.save(profile);
            }
            case CLOSED -> {
                profile.close();
                accountProfilePort.save(profile);
            }
            case ACTIVE -> {
                profile.activate();
                accountProfilePort.save(profile);
            }
        }
    }
}
