package com.kori.adapters.out.jpa.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kori.adapters.out.jpa.repo.IdempotencyJpaRepository;
import com.kori.application.port.out.IdempotencyPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Component
public class JpaIdempotencyAdapter implements IdempotencyPort {

    private final IdempotencyJpaRepository repo;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public JpaIdempotencyAdapter(IdempotencyJpaRepository repo, ObjectMapper objectMapper) {
        this.repo = Objects.requireNonNull(repo);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public <T> Optional<T> find(String idempotencyKey, Class<T> type) {
        return repo.findById(idempotencyKey).map(r -> {
            if (!r.getResultType().equals(type.getName())) {
                throw new IllegalStateException("Idempotency type mismatch for key=" + idempotencyKey);
            }
            try {
                return objectMapper.readValue(r.getResultJson(), type);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to deserialize idempotency for key=" + idempotencyKey, e);
            }
        });
    }

    @Override
    @Transactional
    public void save(String idempotencyKey, Object result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            String type = result.getClass().getName();

            // INSERT ONLY (first write wins)
            em.createNativeQuery("""
                INSERT INTO idempotency_records (idempotency_key, result_type, result_json)
                VALUES (?1, ?2, ?3)
                ON CONFLICT (idempotency_key) DO NOTHING
            """)
                    .setParameter(1, idempotencyKey)
                    .setParameter(2, type)
                    .setParameter(3, json)
                    .executeUpdate();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to save idempotency key=" + idempotencyKey, e);
        }
    }
}
