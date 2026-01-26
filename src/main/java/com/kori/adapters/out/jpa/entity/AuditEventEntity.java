package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "audit_events",
        indexes = {
                @Index(name = "idx_audit_occurred_at", columnList = "occurred_at"),
                @Index(name = "idx_audit_action", columnList = "action")
        })
@Access(AccessType.FIELD)
public class AuditEventEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "action", nullable = false, length = 128)
    private String action;

    @Column(name = "actor_type", nullable = false, length = 32)
    private String actorType;

    @Column(name = "actor_id", nullable = false, length = 128)
    private String actorId;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "metadata_json", nullable = false, columnDefinition = "text")
    private String metadataJson;

    protected AuditEventEntity() { }

    public AuditEventEntity(UUID id, String action, String actorType, String actorId, OffsetDateTime occurredAt, String metadataJson) {
        this.id = id;
        this.action = action;
        this.actorType = actorType;
        this.actorId = actorId;
        this.occurredAt = occurredAt;
        this.metadataJson = metadataJson;
    }

}
