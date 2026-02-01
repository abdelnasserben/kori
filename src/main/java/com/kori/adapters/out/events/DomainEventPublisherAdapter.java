package com.kori.adapters.out.events;

import com.kori.application.events.DomainEvent;
import com.kori.application.port.out.DomainEventPublisherPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Adapter Spring pour publier des événements de domaine/application via
 * {@link ApplicationEventPublisher}.
 */
@Component
public class DomainEventPublisherAdapter implements DomainEventPublisherPort {

    private final ApplicationEventPublisher publisher;

    public DomainEventPublisherAdapter(ApplicationEventPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher);
    }

    @Override
    public void publish(DomainEvent event) {
        publisher.publishEvent(Objects.requireNonNull(event, "event"));
    }
}
