package com.kori.application.port.out;

import com.kori.domain.model.card.HashedPin;

public interface PinHasherPort {
    HashedPin hash(String rawPin);

    boolean matches(String rawPin, HashedPin hashedPin);
}
