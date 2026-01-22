package com.kori.adapters.out.security;

import com.kori.application.port.out.PinHasherPort;
import com.kori.domain.model.card.HashedPin;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public final class BCryptPinHasherAdapter implements PinHasherPort {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Override
    public HashedPin hash(String rawPin) {
        Objects.requireNonNull(rawPin, "rawPin must not be null");
        return new HashedPin(encoder.encode(rawPin));
    }

    @Override
    public boolean matches(String rawPin, HashedPin hashedPin) {
        Objects.requireNonNull(rawPin, "rawPin must not be null");
        Objects.requireNonNull(hashedPin, "hashedPin must not be null");
        return encoder.matches(rawPin, hashedPin.value());
    }
}
