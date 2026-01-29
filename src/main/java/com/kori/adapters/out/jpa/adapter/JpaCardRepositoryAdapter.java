package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.entity.CardEntity;
import com.kori.adapters.out.jpa.repo.CardJpaRepository;
import com.kori.application.port.out.CardRepositoryPort;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardId;
import com.kori.domain.model.card.CardStatus;
import com.kori.domain.model.card.HashedPin;
import com.kori.domain.model.client.ClientId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;

@Component
public class JpaCardRepositoryAdapter implements CardRepositoryPort {

    private final CardJpaRepository repo;

    public JpaCardRepositoryAdapter(CardJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Card> findByCardUid(String cardUid) {
        return repo.findByCardUid(cardUid).map(this::toDomain);
    }

    @Override
    @Transactional
    public Card save(Card card) {
        CardEntity entity = new CardEntity(
                card.id().value(),
                card.clientId().value(),
                card.cardUid(),
                card.hashedPin().value(),
                card.status().name(),
                card.failedPinAttempts(),
                card.createdAt().atOffset(ZoneOffset.UTC)
        );

        repo.save(entity);
        return card;
    }

    private Card toDomain(CardEntity e) {
        return new Card(
                new CardId(e.getId()),
                new ClientId(e.getClientId()),
                e.getCardUid(),
                new HashedPin(e.getHashedPin()),
                CardStatus.valueOf(e.getStatus()),
                e.getFailedPinAttempts(),
                e.getCreatedAt().toInstant()
        );
    }
}
