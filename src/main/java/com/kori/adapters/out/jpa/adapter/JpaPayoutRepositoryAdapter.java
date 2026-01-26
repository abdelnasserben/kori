package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.entity.PayoutEntity;
import com.kori.adapters.out.jpa.repo.PayoutJpaRepository;
import com.kori.application.port.out.PayoutRepositoryPort;
import com.kori.domain.model.agent.AgentId;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.payout.Payout;
import com.kori.domain.model.payout.PayoutId;
import com.kori.domain.model.transaction.TransactionId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaPayoutRepositoryAdapter implements PayoutRepositoryPort {

    private final PayoutJpaRepository repo;

    public JpaPayoutRepositoryAdapter(PayoutJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo, "repo");
    }

    @Override
    @Transactional
    public Payout save(Payout payout) {
        Objects.requireNonNull(payout, "payout");

        PayoutEntity entity = new PayoutEntity(
                payout.id().value(),
                payout.agentId().toString(),                 // AgentId -> String
                payout.transactionId().toString(),           // TransactionId -> String
                payout.amount().asBigDecimal(),
                payout.status(),
                payout.createdAt(),
                payout.completedAt(),
                payout.failedAt(),
                payout.failureReason()
        );

        repo.save(entity);
        return payout;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payout> findById(PayoutId payoutId) {
        Objects.requireNonNull(payoutId, "payoutId");
        return repo.findById(UUID.fromString(payoutId.toString())).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsRequestedForAgent(AgentId agentId) {
        Objects.requireNonNull(agentId, "agentId");
        return repo.existsRequestedForAgent(agentId.toString());
    }

    private Payout toDomain(PayoutEntity e) {
        return new Payout(
                new PayoutId(e.getId()),
                AgentId.of(e.getAgentId()),
                TransactionId.of(e.getTransactionId()),
                Money.of(e.getAmount()),
                e.getStatus(),
                e.getCreatedAt(),
                e.getCompletedAt(),
                e.getFailedAt(),
                e.getFailureReason()
        );
    }
}
