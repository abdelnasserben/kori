package com.kori.application.events;

import java.time.Instant;

/**
 * Contrat minimal pour tous les événements applicatifs (ex: changements de statut).
 * Utile pour idempotency/outbox et pour standardiser audit/debug.
 */
public interface DomainEvent {

    /**
     * Identifiant unique de l'événement (UUID string par ex).
     */
    String eventId();

    /**
     * Date de création de l'événement.
     */
    Instant occurredAt();
}
