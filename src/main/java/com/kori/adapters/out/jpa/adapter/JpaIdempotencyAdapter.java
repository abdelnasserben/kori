package com.kori.adapters.out.jpa.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kori.adapters.out.jpa.entity.IdempotencyRecordEntity;
import com.kori.adapters.out.jpa.repo.IdempotencyJpaRepository;
import com.kori.application.exception.IdempotencyConflictException;
import com.kori.application.idempotency.IdempotencyClaim;
import com.kori.application.idempotency.IdempotencyStatus;
import com.kori.application.port.out.IdempotencyPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class JpaIdempotencyAdapter implements IdempotencyPort {

    private final IdempotencyJpaRepository repo;
    private final ObjectMapper objectMapper;
    private final Duration idempotencyTtl;

    @PersistenceContext
    private EntityManager em;

    public JpaIdempotencyAdapter(
            IdempotencyJpaRepository repo,
            ObjectMapper objectMapper,
            @Value("${kori.idempotency.ttl:PT24H}") Duration idempotencyTtl) {
        this.repo = Objects.requireNonNull(repo);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.idempotencyTtl = idempotencyTtl;
    }

    @Override
    @Transactional
    public <T> IdempotencyClaim<T> claimOrLoad(String idempotencyKey, String requestHash, Class<T> type) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime expiresAt = now.plus(idempotencyTtl);

        int claimed = em.createNativeQuery("""
                INSERT INTO idempotency_records (idempotency_key, result_type, result_json, request_hash, status, expires_at)
                VALUES (?1, ?2, NULL, ?3, 'IN_PROGRESS', ?4)
                ON CONFLICT (idempotency_key) DO UPDATE
                    SET request_hash = EXCLUDED.request_hash,
                        result_type = EXCLUDED.result_type,
                        result_json = NULL,
                        status = 'IN_PROGRESS',
                        expires_at = EXCLUDED.expires_at
                    WHERE idempotency_records.expires_at < ?5
                """)
                .setParameter(1, idempotencyKey)
                .setParameter(2, type.getName())
                .setParameter(3, requestHash)
                .setParameter(4, expiresAt)
                .setParameter(5, now)
                .executeUpdate();

        if (claimed > 0) {
            return IdempotencyClaim.claimed();
        }

        return repo.findById(idempotencyKey)
                .map(record -> mapExistingRecord(idempotencyKey, requestHash, type, record))
                .orElseGet(() -> attemptClaimAfterDelete(idempotencyKey, requestHash, type, expiresAt));
    }

    @Override
    @Transactional
    public void complete(String idempotencyKey, String requestHash, Object result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            String type = result.getClass().getName();
            OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plus(idempotencyTtl);

            int updated = em.createNativeQuery("""
                UPDATE idempotency_records
                SET result_type = ?2,
                    result_json = ?3,
                    request_hash = ?4,
                    status = 'COMPLETED',
                    expires_at = ?5
                WHERE idempotency_key = ?1
                  AND request_hash = ?4
                  AND status = 'IN_PROGRESS'
                """)
                    .setParameter(1, idempotencyKey)
                    .setParameter(2, type)
                    .setParameter(3, json)
                    .setParameter(4, requestHash)
                    .setParameter(5, expiresAt)
                    .executeUpdate();
            if (updated == 0) {
                handleCompletionNoUpdate(idempotencyKey, requestHash, type);
            }

        } catch (Exception e) {
            throw new IllegalStateException("Failed to complete idempotency key=" + idempotencyKey, e);
        }
    }

    @Override
    @Transactional
    public void fail(String idempotencyKey, String requestHash) {
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plus(idempotencyTtl);

        int updated = em.createNativeQuery("""
                UPDATE idempotency_records
                SET status = 'FAILED',
                    expires_at = ?3
                WHERE idempotency_key = ?1
                  AND request_hash = ?2
                  AND status = 'IN_PROGRESS'
            """)
                .setParameter(1, idempotencyKey)
                .setParameter(2, requestHash)
                .setParameter(3, expiresAt)
                .executeUpdate();

        if (updated == 0) {
            repo.findById(idempotencyKey)
                    .filter(record -> record.getRequestHash() != null
                            && !record.getRequestHash().isBlank()
                            && !record.getRequestHash().equals(requestHash))
                    .ifPresent(record -> {
                        throw new IdempotencyConflictException(
                                "Idempotency key reuse with different payload.",
                                Map.of("idempotencyKey", idempotencyKey)
                        );
                    });
        }
    }

    private <T> IdempotencyClaim<T> mapExistingRecord(String idempotencyKey,
                                                      String requestHash,
                                                      Class<T> type,
                                                      IdempotencyRecordEntity record) {
        if (record.getExpiresAt() != null && record.getExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            repo.deleteById(idempotencyKey);
            return attemptClaimAfterDelete(idempotencyKey, requestHash, type, OffsetDateTime.now(ZoneOffset.UTC).plus(idempotencyTtl));
        }
        if (record.getRequestHash() != null && !record.getRequestHash().isBlank() && !record.getRequestHash().equals(requestHash)) {
            return IdempotencyClaim.conflict();
        }
        if (!record.getResultType().equals(type.getName())) {
            throw new IllegalStateException("Idempotency type mismatch for key=" + idempotencyKey);
        }

        if (record.getStatus() == IdempotencyStatus.COMPLETED && record.getResultJson() != null && !record.getResultJson().isBlank()) {
            try {
                return IdempotencyClaim.completed(objectMapper.readValue(record.getResultJson(), type));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to deserialize idempotency for key=" + idempotencyKey, e);
            }
        }

        if (record.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            return IdempotencyClaim.inProgress();
        }

        return IdempotencyClaim.conflict();
    }

    private <T> IdempotencyClaim<T> attemptClaimAfterDelete(String idempotencyKey,
                                                            String requestHash,
                                                            Class<T> type,
                                                            OffsetDateTime expiresAt) {
        int claimed = em.createNativeQuery("""
                INSERT INTO idempotency_records (idempotency_key, result_type, result_json, request_hash, status, expires_at)
                VALUES (?1, ?2, NULL, ?3, 'IN_PROGRESS', ?4)
                ON CONFLICT (idempotency_key) DO NOTHING
                """)
                .setParameter(1, idempotencyKey)
                .setParameter(2, type.getName())
                .setParameter(3, requestHash)
                .setParameter(4, expiresAt)
                .executeUpdate();
        if (claimed > 0) {
            return IdempotencyClaim.claimed();
        }
        return IdempotencyClaim.inProgress();
    }

    private void handleCompletionNoUpdate(String idempotencyKey, String requestHash, String type) {
        Optional<IdempotencyRecordEntity> record = repo.findById(idempotencyKey);
        if (record.isEmpty()) {
            throw new IllegalStateException("Missing idempotency record for key=" + idempotencyKey);
        }
        if (record.get().getRequestHash() != null
                && !record.get().getRequestHash().isBlank()
                && !record.get().getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(
                    "Idempotency key reuse with different payload.",
                    Map.of("idempotencyKey", idempotencyKey)
            );
        }
        if (!record.get().getResultType().equals(type)) {
            throw new IllegalStateException("Idempotency type mismatch for key=" + idempotencyKey);
        }
    }
}
