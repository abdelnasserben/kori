package com.kori.adapters.in.events;

import com.kori.application.events.AgentStatusChangedEvent;
import com.kori.application.handler.OnAgentStatusChangedHandler;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
public class AgentStatusChangedEventListener {

    private final OnAgentStatusChangedHandler handler;

    public AgentStatusChangedEventListener(OnAgentStatusChangedHandler handler) {
        this.handler = Objects.requireNonNull(handler);
    }

    @EventListener
    @Transactional
    public void on(AgentStatusChangedEvent event) {
        handler.handle(event);
    }
}
