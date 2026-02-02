package com.kori.application.exception;

import java.util.Map;

public class InvalidPinFormatException extends ValidationException {
    public InvalidPinFormatException(String message) {
        super(ApplicationErrorCode.INVALID_PIN_FORMAT, message);
    }

    public InvalidPinFormatException(String message, Map<String, Object> metadata) {
        super(ApplicationErrorCode.INVALID_PIN_FORMAT, message, metadata);
    }
}
