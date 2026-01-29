package com.kori.application.handler;

import com.kori.application.events.MerchantStatusChangedEvent;
import com.kori.application.port.out.AccountProfilePort;
import com.kori.application.port.out.TerminalRepositoryPort;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.MerchantId;
import com.kori.domain.model.terminal.Terminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnMerchantStatusChangedHandlerTest {

    @Mock AccountProfilePort accountProfilePort;
    @Mock TerminalRepositoryPort terminalRepositoryPort;
    @InjectMocks OnMerchantStatusChangedHandler handler;

    private final MerchantId merchantId = new MerchantId(UUID.randomUUID());


    @Test
    void suspend_merchant_suspends_account_and_active_terminals_only() {
        AccountProfile profile = mock(AccountProfile.class);
        when(accountProfilePort.findByAccount(merchantRef()))
                .thenReturn(Optional.of(profile));

        Terminal active = terminal(Status.ACTIVE);
        Terminal closed = terminal(Status.CLOSED);
        Terminal suspended = terminal(Status.SUSPENDED);

        when(terminalRepositoryPort.findByMerchantId(merchantId))
                .thenReturn(List.of(active, closed, suspended));

        handler.handle(event(Status.SUSPENDED));

        verify(profile).suspend();
        verify(accountProfilePort).save(profile);

        verify(active).suspend();
        verify(terminalRepositoryPort).save(active);

        verify(closed, never()).suspend();
        verify(suspended, never()).suspend();
    }

    @Test
    void close_merchant_closes_account_and_closes_terminals_except_already_closed() {
        AccountProfile profile = mock(AccountProfile.class);
        when(accountProfilePort.findByAccount(merchantRef()))
                .thenReturn(Optional.of(profile));

        Terminal active = terminal(Status.ACTIVE);
        Terminal suspended = terminal(Status.SUSPENDED);
        Terminal closed = terminal(Status.CLOSED);

        when(terminalRepositoryPort.findByMerchantId(merchantId))
                .thenReturn(List.of(active, suspended, closed));

        handler.handle(event(Status.CLOSED));

        verify(profile).close();
        verify(accountProfilePort).save(profile);

        verify(active).close();
        verify(suspended).close();
        verify(terminalRepositoryPort).save(active);
        verify(terminalRepositoryPort).save(suspended);

        verify(closed, never()).close();
    }

    @Test
    void no_account_profile_does_not_fail_and_terminals_are_processed() {
        when(accountProfilePort.findByAccount(merchantRef()))
                .thenReturn(Optional.empty());

        Terminal active = terminal(Status.ACTIVE);
        when(terminalRepositoryPort.findByMerchantId(merchantId))
                .thenReturn(List.of(active));

        handler.handle(event(Status.SUSPENDED));

        verify(active).suspend();
        verify(terminalRepositoryPort).save(active);

        verifyNoMoreInteractions(accountProfilePort);
    }

    @Test
    void no_action_when_status_does_not_change() {
        handler.handle(event(Status.ACTIVE));

        verifyNoInteractions(accountProfilePort);
        verifyNoInteractions(terminalRepositoryPort);
    }

    // HELPERS

    private MerchantStatusChangedEvent event(Status after) {
        return new MerchantStatusChangedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                merchantId,
                Status.ACTIVE,
                after,
                "test"
        );
    }

    private LedgerAccountRef merchantRef() {
        return LedgerAccountRef.merchant(merchantId.value().toString());
    }

    private Terminal terminal(Status status) {
        Terminal terminal = mock(Terminal.class);
        when(terminal.status()).thenReturn(status);
        return terminal;
    }
}
