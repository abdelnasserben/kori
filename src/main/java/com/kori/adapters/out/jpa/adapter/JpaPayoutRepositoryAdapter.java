package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.entity.PayoutEntity;
import com.kori.adapters.out.jpa.repo.PayoutJpaRepository;
import com.kori.application.port.out.PayoutRepositoryPort;
import com.kori.domain.model.payout.Payout;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Component
public class JpaPayoutRepositoryAdapter implements PayoutRepositoryPort {

    private final PayoutJpaRepository repo;

    public JpaPayoutRepositoryAdapter(PayoutJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Override
    @Transactional
    public Payout save(Payout payout) {
        Objects.requireNonNull(payout, "payout must not be null");

        PayoutEntity entity = new PayoutEntity(
                payout.id().value(),
                payout.agentId(),
                UUID.fromString(payout.transactionId().value()),
                payout.amount().asBigDecimal(),
                payout.status().name(),
                payout.createdAt().atOffset(ZoneOffset.UTC),
                payout.completedAt() == null ? null : payout.completedAt().atOffset(ZoneOffset.UTC)
        );

        repo.save(entity);
        return payout;
    }

}
