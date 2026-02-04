package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.repo.AuditEventJpaRepository;
import com.kori.adapters.out.jpa.repo.IdempotencyJpaRepository;
import com.kori.application.port.out.DataRetentionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Component
public class JpaDataRetentionAdapter implements DataRetentionPort {

    private final IdempotencyJpaRepository idempotencyRepository;
    private final AuditEventJpaRepository auditEventRepository;

    public JpaDataRetentionAdapter(
            IdempotencyJpaRepository idempotencyRepository,
            AuditEventJpaRepository auditEventRepository
    ) {
        this.idempotencyRepository = Objects.requireNonNull(idempotencyRepository, "idempotencyRepository");
        this.auditEventRepository = Objects.requireNonNull(auditEventRepository, "auditEventRepository");
    }

    @Override
    @Transactional
    public long purgeExpiredIdempotencyRecords(Instant now) {
        OffsetDateTime cutoff = OffsetDateTime.ofInstant(now, ZoneOffset.UTC);
        return idempotencyRepository.deleteByExpiresAtBefore(cutoff);
    }

    @Override
    @Transactional
    public long purgeAuditEventsBefore(Instant cutoff) {
        OffsetDateTime cutoffTime = OffsetDateTime.ofInstant(cutoff, ZoneOffset.UTC);
        return auditEventRepository.deleteByOccurredAtBefore(cutoffTime);
    }
}
