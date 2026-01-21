package com.kori.application.exception;

public class ForbiddenOperationException extends ApplicationException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
