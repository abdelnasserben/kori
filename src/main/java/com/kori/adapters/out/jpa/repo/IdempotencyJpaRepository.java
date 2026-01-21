package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.IdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyJpaRepository extends JpaRepository<IdempotencyRecordEntity, String> {
}
