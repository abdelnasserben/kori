package com.kori.application.handler;

import com.kori.application.events.ClientStatusChangedEvent;
import com.kori.application.port.out.AccountProfilePort;
import com.kori.application.port.out.CardRepositoryPort;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardStatus;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Status;
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
class OnClientStatusChangedHandlerTest {

    @Mock AccountProfilePort accountProfilePort;
    @Mock CardRepositoryPort cardRepositoryPort;
    @InjectMocks OnClientStatusChangedHandler handler;

    private final ClientId clientId = new ClientId(UUID.randomUUID());

    @Test
    void suspend_client_suspends_account_and_active_cards_only() {
        AccountProfile profile = mock(AccountProfile.class);
        when(accountProfilePort.findByAccount(clientRef()))
                .thenReturn(Optional.of(profile));

        Card activeCard = card(CardStatus.ACTIVE);
        Card lostCard = card(CardStatus.LOST);
        Card blockedCard = card(CardStatus.BLOCKED);

        when(cardRepositoryPort.findByClientId(clientId))
                .thenReturn(List.of(activeCard, lostCard, blockedCard));

        handler.handle(event(Status.SUSPENDED));

        verify(profile).suspend();
        verify(accountProfilePort).save(profile);

        verify(activeCard).suspend();
        verify(cardRepositoryPort).save(activeCard);

        verify(lostCard, never()).suspend();
        verify(blockedCard, never()).suspend();
    }

    @Test
    void close_client_closes_account_and_inactivates_cards_except_lost() {
        AccountProfile profile = mock(AccountProfile.class);
        when(accountProfilePort.findByAccount(clientRef()))
                .thenReturn(Optional.of(profile));

        Card activeCard = card(CardStatus.ACTIVE);
        Card suspendedCard = card(CardStatus.SUSPENDED);
        Card lostCard = card(CardStatus.LOST);

        when(cardRepositoryPort.findByClientId(clientId))
                .thenReturn(List.of(activeCard, suspendedCard, lostCard));

        handler.handle(event(Status.CLOSED));

        verify(profile).close();
        verify(accountProfilePort).save(profile);

        verify(activeCard).deactivate();
        verify(suspendedCard).deactivate();

        verify(cardRepositoryPort).save(activeCard);
        verify(cardRepositoryPort).save(suspendedCard);

        verify(lostCard, never()).deactivate();
    }

    @Test
    void no_account_profile_does_not_fail_and_cards_are_processed() {
        when(accountProfilePort.findByAccount(clientRef()))
                .thenReturn(Optional.empty());

        Card activeCard = card(CardStatus.ACTIVE);
        when(cardRepositoryPort.findByClientId(clientId))
                .thenReturn(List.of(activeCard));

        handler.handle(event(Status.SUSPENDED));

        verify(activeCard).suspend();
        verify(cardRepositoryPort).save(activeCard);
        verifyNoMoreInteractions(accountProfilePort);
    }

    @Test
    void no_action_when_status_does_not_change() {
        handler.handle(event(Status.ACTIVE));

        verifyNoInteractions(accountProfilePort);
        verifyNoInteractions(cardRepositoryPort);
    }

    // HELPERS

    private ClientStatusChangedEvent event(Status after) {
        return new ClientStatusChangedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                clientId,
                Status.ACTIVE,
                after,
                "test"
        );
    }

    private LedgerAccountRef clientRef() {
        return LedgerAccountRef.client(clientId.value().toString());
    }

    private Card card(CardStatus status) {
        Card card = mock(Card.class);
        when(card.status()).thenReturn(status);
        return card;
    }
}
