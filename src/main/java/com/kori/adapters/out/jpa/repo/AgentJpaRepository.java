package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.AgentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentJpaRepository extends JpaRepository<AgentEntity, String> {
    Optional<AgentEntity> findByCode(String code);
    boolean existsByCode(String code);
}
