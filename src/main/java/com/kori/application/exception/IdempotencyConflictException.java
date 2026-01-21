package com.kori.application.exception;

public class IdempotencyConflictException extends ApplicationException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}
