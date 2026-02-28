package com.kori.adapters.out.jpa.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kori.adapters.out.jpa.entity.AuditEventEntity;
import com.kori.adapters.out.jpa.repo.AuditEventJpaRepository;
import com.kori.application.port.out.AuditPort;
import com.kori.domain.model.audit.AuditEvent;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Component
public class JpaAuditAdapter implements AuditPort {

    private final AuditEventJpaRepository repo;
    private final ObjectMapper objectMapper;

    public JpaAuditAdapter(AuditEventJpaRepository repo, ObjectMapper objectMapper) {
        this.repo = Objects.requireNonNull(repo);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    @Transactional
    public void publish(AuditEvent event) {
        try {
            String correlationId = MDC.get("correlationId");
            Map<String, String> metadata = AuditResourceMetadataHelper.enrich(event.action(), event.metadata());
            if (correlationId != null && !correlationId.isBlank() && !metadata.containsKey("correlationId")) {
                metadata.put("correlationId", correlationId);
            }

            String metadataJson = objectMapper.writeValueAsString(metadata);
            AuditEventEntity entity = new AuditEventEntity(
                    UUID.randomUUID(),
                    event.action(),
                    event.actorType(),
                    event.actorRef(),
                    event.occurredAt().atOffset(java.time.ZoneOffset.UTC),
                    metadataJson
            );
            repo.save(entity);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to persist audit event", e);
        }
    }
}
