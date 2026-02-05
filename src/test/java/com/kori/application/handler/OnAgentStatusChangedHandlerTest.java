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
        AccountProfile wallet = mock(AccountProfile.class);
        AccountProfile clearing = mock(AccountProfile.class);
        when(accountProfilePort.findByAccount(walletRef())).thenReturn(Optional.of(wallet));
        when(accountProfilePort.findByAccount(clearingRef())).thenReturn(Optional.of(clearing));

        handler.handle(event(Status.ACTIVE, Status.SUSPENDED));

        verify(wallet).suspend();
        verify(clearing).suspend();
        verify(accountProfilePort).save(wallet);
        verify(accountProfilePort).save(clearing);
    }

    @Test
    void close_agent_closes_account_profile() {
        AccountProfile wallet = mock(AccountProfile.class);
        AccountProfile clearing = mock(AccountProfile.class);
        when(accountProfilePort.findByAccount(walletRef())).thenReturn(Optional.of(wallet));
        when(accountProfilePort.findByAccount(clearingRef())).thenReturn(Optional.of(clearing));

        handler.handle(event(Status.ACTIVE, Status.CLOSED));

        verify(wallet).close();
        verify(clearing).close();
        verify(accountProfilePort).save(wallet);
        verify(accountProfilePort).save(clearing);
    }

    @Test
    void activate_agent_activates_account_profile() {
        AccountProfile wallet = mock(AccountProfile.class);
        AccountProfile clearing = mock(AccountProfile.class);
        when(accountProfilePort.findByAccount(walletRef())).thenReturn(Optional.of(wallet));
        when(accountProfilePort.findByAccount(clearingRef())).thenReturn(Optional.of(clearing));

        handler.handle(event(Status.SUSPENDED, Status.ACTIVE));

        verify(wallet).activate();
        verify(clearing).activate();
        verify(accountProfilePort).save(wallet);
        verify(accountProfilePort).save(clearing);
    }

    @Test
    void no_account_profile_does_nothing() {
        when(accountProfilePort.findByAccount(walletRef())).thenReturn(Optional.empty());
        when(accountProfilePort.findByAccount(clearingRef())).thenReturn(Optional.empty());

        handler.handle(event(Status.ACTIVE, Status.SUSPENDED));

        verify(accountProfilePort).findByAccount(walletRef());
        verify(accountProfilePort).findByAccount(clearingRef());
        verify(accountProfilePort, never()).save(any());
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

    private LedgerAccountRef walletRef() {
        return LedgerAccountRef.agentWallet(agentId.value().toString());
    }

    private LedgerAccountRef clearingRef() {
        return LedgerAccountRef.agentCashClearing(agentId.value().toString());
    }
}
