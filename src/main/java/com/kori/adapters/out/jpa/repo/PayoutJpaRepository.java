package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.PayoutEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PayoutJpaRepository extends JpaRepository<PayoutEntity, UUID> {
        boolean existsByAgentIdAndStatus(UUID agentId, String status);
}
