package com.kori.application.exception;

import java.util.Map;

public class ValidationException extends ApplicationException {
    public ValidationException(String message) {
        super(ApplicationErrorCode.INVALID_INPUT, ApplicationErrorCategory.VALIDATION, message);
    }

    public ValidationException(String message, Map<String, Object> metadata) {
        super(ApplicationErrorCode.INVALID_INPUT, ApplicationErrorCategory.VALIDATION, message, metadata);
    }

    protected ValidationException(ApplicationErrorCode code, String message) {
        super(code, ApplicationErrorCategory.VALIDATION, message);
    }

    protected ValidationException(ApplicationErrorCode code, String message, Map<String, Object> metadata) {
        super(code, ApplicationErrorCategory.VALIDATION, message, metadata);
    }
}
