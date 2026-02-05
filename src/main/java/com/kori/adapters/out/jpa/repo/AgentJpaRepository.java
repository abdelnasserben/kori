package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.AgentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AgentJpaRepository extends JpaRepository<AgentEntity, UUID> {
    Optional<AgentEntity> findByCode(String code);

    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AgentEntity a where a.id = :id")
    Optional<AgentEntity> findByIdForUpdate(@Param("id") UUID id);
}
