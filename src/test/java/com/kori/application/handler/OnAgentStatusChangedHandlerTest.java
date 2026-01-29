package com.kori.application.handler;

import com.kori.application.events.AgentStatusChangedEvent;
import com.kori.application.port.out.AccountProfilePort;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.agent.AgentId;
import com.kori.domain.model.common.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnAgentStatusChangedHandlerTest {

    @Mock AccountProfilePort accountProfilePort;
    @InjectMocks OnAgentStatusChangedHandler handler;

    private final AgentId agentId = new AgentId(UUID.randomUUID());

    @Test
    void suspend_agent_suspends_account_profile() {
        AccountProfile profile = mock(AccountProfile.class);
        when(accountProfilePort.findByAccount(agentRef()))
                .thenReturn(Optional.of(profile));

        handler.handle(event(Status.ACTIVE, Status.SUSPENDED));

        verify(profile).suspend();
        verify(accountProfilePort).save(profile);
    }

    @Test
    void close_agent_closes_account_profile() {
        AccountProfile profile = mock(AccountProfile.class);
        when(accountProfilePort.findByAccount(agentRef()))
                .thenReturn(Optional.of(profile));

        handler.handle(event(Status.ACTIVE, Status.CLOSED));

        verify(profile).close();
        verify(accountProfilePort).save(profile);
    }

    @Test
    void activate_agent_activates_account_profile() {
        AccountProfile profile = mock(AccountProfile.class);
        when(accountProfilePort.findByAccount(agentRef()))
                .thenReturn(Optional.of(profile));

        handler.handle(event(Status.SUSPENDED, Status.ACTIVE));

        verify(profile).activate();
        verify(accountProfilePort).save(profile);
    }

    @Test
    void no_account_profile_does_nothing() {
        when(accountProfilePort.findByAccount(agentRef()))
                .thenReturn(Optional.empty());

        handler.handle(event(Status.ACTIVE, Status.SUSPENDED));

        verifyNoMoreInteractions(accountProfilePort);
    }

    @Test
    void no_action_when_status_does_not_change() {
        handler.handle(event(Status.ACTIVE, Status.ACTIVE));

        verifyNoInteractions(accountProfilePort);
    }

    // HELPERS

    private AgentStatusChangedEvent event(Status before, Status after) {
        return new AgentStatusChangedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                agentId,
                before,
                after,
                "test"
        );
    }

    private LedgerAccountRef agentRef() {
        return LedgerAccountRef.agent(agentId.value().toString());
    }
}
