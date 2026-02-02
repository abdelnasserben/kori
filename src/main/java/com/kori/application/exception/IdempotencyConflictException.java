package com.kori.application.exception;

import java.util.Map;

public class IdempotencyConflictException extends ApplicationException {
    public IdempotencyConflictException(String message) {
        super(ApplicationErrorCode.IDEMPOTENCY_CONFLICT, ApplicationErrorCategory.CONFLICT, message);
    }

    public IdempotencyConflictException(String message, Map<String, Object> metadata) {
        super(ApplicationErrorCode.IDEMPOTENCY_CONFLICT, ApplicationErrorCategory.CONFLICT, message, metadata);
    }
}
