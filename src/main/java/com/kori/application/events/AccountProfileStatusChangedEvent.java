package com.kori.application.events;

import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.common.Status;

import java.time.Instant;
import java.util.Objects;

/**
 * Événement publié quand le statut d'un AccountProfile change.
 */
public record AccountProfileStatusChangedEvent(
        String eventId,
        Instant occurredAt,
        LedgerAccountRef accountRef,
        Status before,
        Status after,
        String reason
) implements DomainEvent {

    public AccountProfileStatusChangedEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(accountRef, "accountRef");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
    }
}
