package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.PayoutEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PayoutJpaRepository extends JpaRepository<PayoutEntity, UUID> {
    Optional<PayoutEntity> findByTransactionId(UUID transactionId);

    boolean existsByAgentIdAndStatus(String agentId, String status);

    boolean existsRequestedForAgent(String agentId);
}
