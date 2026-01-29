package com.kori.application.handler;

import com.kori.application.events.ClientStatusChangedEvent;
import com.kori.application.port.out.AccountProfilePort;
import com.kori.application.port.out.CardRepositoryPort;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardStatus;
import com.kori.domain.model.common.Status;

import java.util.List;
import java.util.Objects;

/**
 * Applique les conséquences métier d'un changement de statut Client :
 * - SUSPENDED → suspend AccountProfile (client) + suspend cards ACTIVE
 * - CLOSED    → close AccountProfile (client) + inactivate cards (sauf LOST)
 *  * Handler pure application (pas de Spring ici).
 */
public class OnClientStatusChangedHandler {

    private final AccountProfilePort accountProfilePort;
    private final CardRepositoryPort cardRepositoryPort;

    public OnClientStatusChangedHandler(AccountProfilePort accountProfilePort,
                                        CardRepositoryPort cardRepositoryPort) {
        this.accountProfilePort = Objects.requireNonNull(accountProfilePort);
        this.cardRepositoryPort = Objects.requireNonNull(cardRepositoryPort);
    }

    public void handle(ClientStatusChangedEvent event) {
        Objects.requireNonNull(event, "event");

        // Rien à faire si pas de changement réel
        if (event.before() == event.after()) {
            return;
        }

        Status after = event.after();

        // 1) Propager vers AccountProfile du client (si existant)
        LedgerAccountRef clientAccountRef = LedgerAccountRef.client(event.clientId().value().toString());
        AccountProfile clientProfile = accountProfilePort.findByAccount(clientAccountRef).orElse(null);

        if (clientProfile != null) {
            switch (after) {
                case SUSPENDED -> {
                    // idempotent côté domaine
                    clientProfile.suspend();
                    accountProfilePort.save(clientProfile);
                }
                case CLOSED -> {
                    clientProfile.close();
                    accountProfilePort.save(clientProfile);
                }
                case ACTIVE -> {
                    // choix métier : si client redevient ACTIVE, on réactive aussi le profile (si pas CLOSED)
                    clientProfile.activate();
                    accountProfilePort.save(clientProfile);
                }
            }
        }

        // 2) Propager vers les cards
        List<Card> cards = cardRepositoryPort.findByClientId(event.clientId());

        switch (after) {
            case SUSPENDED -> {
                // Suspend uniquement les cartes payables (ACTIVE) pour ne pas écraser LOST/BLOCKED/INACTIVE
                for (Card card : cards) {
                    if (card.status() == CardStatus.ACTIVE) {
                        card.suspend();
                        cardRepositoryPort.save(card);
                    }
                }
            }
            case CLOSED -> {
                // Mettre les cartes hors service (sauf LOST qui est terminal)
                for (Card card : cards) {
                    if (card.status() != CardStatus.LOST && card.status() != CardStatus.INACTIVE) {
                        card.deactivate();
                        cardRepositoryPort.save(card);
                    }
                }
            }
        }

        // IMPORTANT : pas de réactivation automatique des cartes
    }
}
