package com.kori.application.idempotency;

import com.kori.application.exception.IdempotencyConflictException;
import com.kori.application.port.out.IdempotencyPort;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class IdempotencyExecutor {

    private final IdempotencyPort idempotencyPort;

    public IdempotencyExecutor(IdempotencyPort idempotencyPort) {
        this.idempotencyPort = Objects.requireNonNull(idempotencyPort, "idempotencyPort");
    }

    public <T> T execute(
            String idempotencyKey,
            String requestHash,
            Class<T> resultType,
            Supplier<T> work
    ) {
        return execute(
                idempotencyKey,
                requestHash,
                resultType,
                work,
                Map.of("idempotencyKey", idempotencyKey)
        );
    }

    public <T> T execute(
            String idempotencyKey,
            String requestHash,
            Class<T> resultType,
            Supplier<T> work,
            Map<String, Object> conflictMetadata
    ) {
        requireNonBlank(idempotencyKey, "idempotencyKey");
        requireNonBlank(requestHash, "requestHash");
        Objects.requireNonNull(resultType, "resultType");
        Objects.requireNonNull(work, "work");

        IdempotencyClaim<T> claim =
                idempotencyPort.claimOrLoad(idempotencyKey, requestHash, resultType);

        switch (claim.status()) {

            case ALREADY_COMPLETED -> {
                return claim.result().orElseThrow(() ->
                        new IllegalStateException(
                                "Idempotency marked ALREADY_COMPLETED but result is missing"
                        )
                );
            }

            case IN_PROGRESS -> throw new IdempotencyConflictException(
                    "Idempotency key is already being processed",
                    conflictMetadata
            );

            case CONFLICT -> throw new IdempotencyConflictException(
                    "Idempotency key reused with a different payload",
                    conflictMetadata
            );

            case CLAIMED -> {
                // We own the execution → continue
            }
        }

        try {
            T result = work.get();
            Objects.requireNonNull(result, "work result must not be null");

            idempotencyPort.complete(idempotencyKey, requestHash, result);
            return result;

        } catch (RuntimeException e) {
            // Best-effort fail marking (never mask the original exception)
            try {
                idempotencyPort.fail(idempotencyKey, requestHash);
            } catch (RuntimeException ignored) {}
            throw e;
        }
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
    }
}
