package com.kori.application.exception;

import java.util.Map;

public final class InsufficientFundsException extends ApplicationException {

    public InsufficientFundsException(String message) {
        super(ApplicationErrorCode.INSUFFICIENT_FUNDS, ApplicationErrorCategory.CONFLICT, message);
    }

    public InsufficientFundsException(String message, Map<String, Object> metadata) {
        super(ApplicationErrorCode.INSUFFICIENT_FUNDS, ApplicationErrorCategory.CONFLICT, message, metadata);
    }
}
