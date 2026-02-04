package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.IdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;

public interface IdempotencyJpaRepository extends JpaRepository<IdempotencyRecordEntity, String> {
    long deleteByExpiresAtBefore(OffsetDateTime cutoff);
}
