package com.kori.adapters.out.jpa.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kori.adapters.out.jpa.repo.IdempotencyJpaRepository;
import com.kori.application.exception.IdempotencyConflictException;
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
    public <T> Optional<T> find(String idempotencyKey, String requestHash, Class<T> type) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return repo.findById(idempotencyKey).flatMap(r -> {
            if (r.getExpiresAt() != null && r.getExpiresAt().isBefore(now)) {
                repo.deleteById(idempotencyKey);
                return Optional.empty();
            }
            if (r.getRequestHash() != null && !r.getRequestHash().isBlank() && !r.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(
                        "Idempotency key reuse with different payload.",
                        Map.of("idempotencyKey", idempotencyKey)
                );
            }
            if (!r.getResultType().equals(type.getName())) {
                throw new IllegalStateException("Idempotency type mismatch for key=" + idempotencyKey);
            }
            try {
                return Optional.of(objectMapper.readValue(r.getResultJson(), type));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to deserialize idempotency for key=" + idempotencyKey, e);
            }
        });
    }

    @Override
    @Transactional
    public void save(String idempotencyKey, String requestHash, Object result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            String type = result.getClass().getName();
            OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plus(idempotencyTtl);

            // INSERT ONLY (first write wins)
            em.createNativeQuery("""
                INSERT INTO idempotency_records (idempotency_key, result_type, result_json, request_hash, expires_at)
                VALUES (?1, ?2, ?3, ?4, ?5)
                ON CONFLICT (idempotency_key) DO NOTHING
            """)
                    .setParameter(1, idempotencyKey)
                    .setParameter(2, type)
                    .setParameter(3, json)
                    .setParameter(4, requestHash)
                    .setParameter(5, expiresAt)
                    .executeUpdate();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to save idempotency key=" + idempotencyKey, e);
        }
    }
}
