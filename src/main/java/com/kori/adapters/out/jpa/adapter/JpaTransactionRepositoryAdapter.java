package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.entity.TransactionEntity;
import com.kori.adapters.out.jpa.repo.TransactionJpaRepository;
import com.kori.application.port.out.TransactionRepositoryPort;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;
import com.kori.domain.model.transaction.TransactionType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaTransactionRepositoryAdapter implements TransactionRepositoryPort {

    private final TransactionJpaRepository repo;

    public JpaTransactionRepositoryAdapter(TransactionJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Override
    @Transactional
    public Transaction save(Transaction transaction) {
        UUID id = transaction.id().value();
        UUID original = transaction.originalTransactionId() == null ? null : transaction.originalTransactionId().value();

        TransactionEntity entity = new TransactionEntity(
                id,
                transaction.type().name(),
                transaction.amount().asBigDecimal(),
                transaction.createdAt().atOffset(ZoneOffset.UTC),
                original
        );

        repo.save(entity);
        return transaction;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Transaction> findById(TransactionId transactionId) {
        return repo.findById(transactionId.value()).map(e ->
                new Transaction(
                        TransactionId.of(e.getId().toString()),
                        TransactionType.valueOf(e.getType()),
                        Money.of(e.getAmount()),
                        e.getCreatedAt().toInstant(),
                        e.getOriginalTransactionId() == null ? null : TransactionId.of(e.getOriginalTransactionId().toString())
                )
        );
    }

    @Override
    public boolean existsReversalFor(TransactionId originalTransactionId) {
        return repo.existsByTypeAndOriginalTransactionId(
                "REVERSAL",
                originalTransactionId.value()
        );
    }
}
