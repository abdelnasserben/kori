package com.kori.application.usecase;

import com.kori.application.exception.IdempotencyConflictException;
import com.kori.application.port.out.IdempotencyPort;

import java.util.Map;
import java.util.Optional;

final class IdempotencyReservations {

    private IdempotencyReservations() {
    }

    static <T> Optional<T> reserveOrLoad(IdempotencyPort idempotencyPort,
                                         String idempotencyKey,
                                         String requestHash,
                                         Class<T> type) {
        if (idempotencyPort.reserve(idempotencyKey, requestHash, type)) {
            return Optional.empty();
        }

        Optional<T> cached = idempotencyPort.find(idempotencyKey, requestHash, type);
        if (cached.isPresent()) {
            return cached;
        }

        throw new IdempotencyConflictException(
                "Idempotency key is already being processed.",
                Map.of("idempotencyKey", idempotencyKey)
        );
    }
}
