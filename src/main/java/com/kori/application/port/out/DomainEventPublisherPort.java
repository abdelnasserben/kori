package com.kori.application.port.out;

import com.kori.application.events.DomainEvent;

/**
 * Publie des événements applicatifs (ex: *StatusChangedEvent*) afin de déclencher des handlers
 * de propagation/consistance.
 *
 * <p>Le domaine reste ignorant de l'infrastructure : l'implémentation est un adapter (ex: Spring
 * {@code ApplicationEventPublisher}).</p>
 */
public interface DomainEventPublisherPort {

    /**
     * Publie un événement applicatif.
     *
     * @param event événement (typiquement un record immutable)
     */
    void publish(DomainEvent event);
}
