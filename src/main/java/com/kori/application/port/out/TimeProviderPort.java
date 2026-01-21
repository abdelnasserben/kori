package com.kori.application.port.out;

import java.time.Instant;

public interface TimeProviderPort {
    Instant now();
}
