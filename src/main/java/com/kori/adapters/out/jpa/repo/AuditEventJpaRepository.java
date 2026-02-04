package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface AuditEventJpaRepository extends JpaRepository<AuditEventEntity, UUID> {
    long deleteByOccurredAtBefore(OffsetDateTime cutoff);
}
