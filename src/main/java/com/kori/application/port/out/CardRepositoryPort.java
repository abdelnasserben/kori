package com.kori.application.port.out;

import com.kori.domain.model.card.Card;

import java.util.Optional;

public interface CardRepositoryPort {
    Optional<Card> findByCardUid(String cardUid);

    Card save(Card card);
}
