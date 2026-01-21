package com.kori.application.port.out;

import java.util.Optional;

public interface IdempotencyPort {
    <T> Optional<T> find(String idempotencyKey, Class<T> type);

    void save(String idempotencyKey, Object result);
}
