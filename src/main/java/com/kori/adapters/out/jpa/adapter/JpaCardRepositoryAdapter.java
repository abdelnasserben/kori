package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.entity.CardEntity;
import com.kori.adapters.out.jpa.repo.CardJpaRepository;
import com.kori.application.port.out.CardRepositoryPort;
import com.kori.domain.model.account.AccountId;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardId;
import com.kori.domain.model.card.CardStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaCardRepositoryAdapter implements CardRepositoryPort {

    private final CardJpaRepository repo;

    public JpaCardRepositoryAdapter(CardJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Card> findByCardUid(String cardUid) {
        Objects.requireNonNull(cardUid, "cardUid must not be null");
        return repo.findByCardUid(cardUid).map(this::toDomain);
    }

    @Override
    @Transactional
    public Card save(Card card) {
        Objects.requireNonNull(card, "card must not be null");

        CardEntity entity = new CardEntity(
                UUID.fromString(card.id().value()),
                UUID.fromString(card.accountId().value()),
                card.cardUid(),
                card.pin(),
                card.status().name(),
                card.failedPinAttempts()
        );

        repo.save(entity);
        return card;
    }

    private Card toDomain(CardEntity e) {
        return new Card(
                CardId.of(e.getId().toString()),
                AccountId.of(e.getAccountId().toString()),
                e.getCardUid(),
                e.getPin(),
                CardStatus.valueOf(e.getStatus()),
                e.getFailedPinAttempts()
        );
    }
}
