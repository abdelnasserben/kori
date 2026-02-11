package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.entity.PayoutEntity;
import com.kori.adapters.out.jpa.repo.PayoutJpaRepository;
import com.kori.application.port.out.PayoutRepositoryPort;
import com.kori.domain.model.agent.AgentId;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.payout.Payout;
import com.kori.domain.model.payout.PayoutId;
import com.kori.domain.model.payout.PayoutStatus;
import com.kori.domain.model.transaction.TransactionId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

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
                payout.agentId().value(),                 // AgentId -> String
                payout.transactionId().value(),           // TransactionId -> String
                payout.amount().asBigDecimal(),
                payout.status().name(),
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
        return repo.findById(payoutId.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsRequestedForAgent(AgentId agentId) {
        Objects.requireNonNull(agentId, "agentCode");
        return repo.existsByAgentIdAndStatus(agentId.value(), PayoutStatus.REQUESTED.name());
    }

    private Payout toDomain(PayoutEntity e) {
        return new Payout(
                new PayoutId(e.getId()),
                new AgentId(e.getAgentId()),
                new TransactionId(e.getTransactionId()),
                Money.of(e.getAmount()),
                PayoutStatus.valueOf(e.getStatus()),
                e.getCreatedAt(),
                e.getCompletedAt(),
                e.getFailedAt(),
                e.getFailureReason()
        );
    }
}
