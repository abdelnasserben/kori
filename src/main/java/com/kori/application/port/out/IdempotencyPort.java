package com.kori.application.port.out;

import java.util.Optional;

public interface IdempotencyPort {
    <T> Optional<T> find(String idempotencyKey, String requestHash, Class<T> type);

    void save(String idempotencyKey, String requestHash, Object result);
}
