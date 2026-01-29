package com.kori.application.events;

import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Status;

import java.time.Instant;
import java.util.Objects;

/**
 * Événement publié quand le statut d'un Client change.
 */
public record ClientStatusChangedEvent(
        String eventId,
        Instant occurredAt,
        ClientId clientId,
        Status before,
        Status after,
        String reason
) implements DomainEvent {

    public ClientStatusChangedEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        // reason peut être null
    }
}
