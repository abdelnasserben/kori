package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.AgentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentJpaRepository extends JpaRepository<AgentEntity, String> {
}
