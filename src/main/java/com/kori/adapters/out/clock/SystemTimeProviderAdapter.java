package com.kori.adapters.out.clock;

import com.kori.application.port.out.TimeProviderPort;

import java.time.Instant;

public final class SystemTimeProviderAdapter implements TimeProviderPort {
    @Override
    public Instant now() {
        return Instant.now();
    }
}
