package com.kori.application.events;

import com.kori.domain.model.agent.AgentId;
import com.kori.domain.model.common.Status;

import java.time.Instant;
import java.util.Objects;

/**
 * Événement publié quand le statut d'un Agent change.
 */
public record AgentStatusChangedEvent(
        String eventId,
        Instant occurredAt,
        AgentId agentId,
        Status before,
        Status after,
        String reason
) implements DomainEvent {

    public AgentStatusChangedEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(agentId, "agentId");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
    }
}
