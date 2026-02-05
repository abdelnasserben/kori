package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.ClientRefundEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClientRefundJpaRepository extends JpaRepository<ClientRefundEntity, UUID> {
    boolean existsByClientIdAndStatus(UUID clientId, String status);
}
