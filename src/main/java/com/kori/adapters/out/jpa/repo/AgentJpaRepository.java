package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.AgentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgentJpaRepository extends JpaRepository<AgentEntity, UUID> {
    Optional<AgentEntity> findByCode(String code);
    boolean existsByCode(String code);
}
