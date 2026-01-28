package com.kori.adapters.out.random;

import com.kori.application.port.out.IdGeneratorPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UuidIdGeneratorAdapter implements IdGeneratorPort {

    @Override
    public UUID newUuid() {
        return UUID.randomUUID();
    }
}
