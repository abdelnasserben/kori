package com.kori.integration.fixture;

import com.kori.adapters.out.jpa.entity.TransactionEntity;
import com.kori.adapters.out.jpa.repo.TransactionJpaRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Fixture JPA dédiée à "transactions".
 *
 * Objectifs :
 * - Créer des transactions via JPA (stable, pas de SQL fragile)
 * - Centraliser la création (id, type, amount, createdAt, originalTransactionId)
 *
 * Note: les tests étant @Transactional, tout est rollback automatiquement.
 */
public final class TransactionFixture {

    private final TransactionJpaRepository transactionJpaRepository;

    public TransactionFixture(TransactionJpaRepository transactionJpaRepository) {
        this.transactionJpaRepository = transactionJpaRepository;
    }

    public TransactionEntity create(String type, BigDecimal amount, OffsetDateTime createdAt) {
        return create(UUID.randomUUID(), type, amount, createdAt, null);
    }

    public TransactionEntity createWithOriginal(String type,
                                                BigDecimal amount,
                                                OffsetDateTime createdAt,
                                                UUID originalTransactionId) {
        return create(UUID.randomUUID(), type, amount, createdAt, originalTransactionId);
    }

    public TransactionEntity create(UUID transactionId,
                                    String type,
                                    BigDecimal amount,
                                    OffsetDateTime createdAt,
                                    UUID originalTransactionId) {
        TransactionEntity entity = new TransactionEntity(
                transactionId,
                type,
                amount,
                createdAt,
                originalTransactionId
        );
        return transactionJpaRepository.saveAndFlush(entity);
    }
}
