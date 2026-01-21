package com.kori.adapters.out.idempotency;

import com.kori.application.port.out.IdempotencyPort;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryIdempotencyAdapter implements IdempotencyPort {

    private final ConcurrentHashMap<String, Object> store = new ConcurrentHashMap<>();

    @Override
    public <T> Optional<T> find(String idempotencyKey, Class<T> type) {
        Object value = store.get(idempotencyKey);
        if (value == null) return Optional.empty();
        if (!type.isInstance(value)) return Optional.empty();
        return Optional.of(type.cast(value));
    }

    @Override
    public void save(String idempotencyKey, Object result) {
        store.put(idempotencyKey, result);
    }
}
