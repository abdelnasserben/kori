package com.kori.application.port.out;

import com.kori.application.idempotency.IdempotencyClaim;

public interface IdempotencyPort {
    <T> IdempotencyClaim<T> claimOrLoad(String idempotencyKey, String requestHash, Class<T> type);

    void complete(String idempotencyKey, String requestHash, Object result);

    void fail(String idempotencyKey, String requestHash);
}
