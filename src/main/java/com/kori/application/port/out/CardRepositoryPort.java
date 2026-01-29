package com.kori.application.port.out;

import com.kori.domain.model.card.Card;
import com.kori.domain.model.client.ClientId;

import java.util.List;
import java.util.Optional;

public interface CardRepositoryPort {
    Optional<Card> findByCardUid(String cardUid);

    Card save(Card card);

    List<Card> findByClientId(ClientId clientId);
}
