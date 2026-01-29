package com.kori.adapters.in.events;

import com.kori.application.events.ClientStatusChangedEvent;
import com.kori.application.handler.OnClientStatusChangedHandler;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Listener Spring qui reçoit l'événement et délègue au handler applicatif.
 */
@Component
public class ClientStatusChangedEventListener {

    private final OnClientStatusChangedHandler handler;

    public ClientStatusChangedEventListener(OnClientStatusChangedHandler handler) {
        this.handler = Objects.requireNonNull(handler);
    }

    @EventListener
    @Transactional
    public void on(ClientStatusChangedEvent event) {
        handler.handle(event);
    }
}
