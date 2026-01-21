package com.kori.adapters.out.clock;

import com.kori.application.port.out.TimeProviderPort;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public final class SystemTimeProviderAdapter implements TimeProviderPort {
    @Override
    public Instant now() {
        return Instant.now();
    }
}
