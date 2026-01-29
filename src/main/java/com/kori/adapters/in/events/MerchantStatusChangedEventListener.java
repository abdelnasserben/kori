package com.kori.adapters.in.events;

import com.kori.application.events.MerchantStatusChangedEvent;
import com.kori.application.handler.OnMerchantStatusChangedHandler;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
public class MerchantStatusChangedEventListener {

    private final OnMerchantStatusChangedHandler handler;

    public MerchantStatusChangedEventListener(OnMerchantStatusChangedHandler handler) {
        this.handler = Objects.requireNonNull(handler);
    }

    @EventListener
    @Transactional
    public void on(MerchantStatusChangedEvent event) {
        handler.handle(event);
    }
}
