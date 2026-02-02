package com.kori.application.exception;

import java.util.Map;

public final class NotFoundException extends ApplicationException {
    public NotFoundException(String message) {
        super(ApplicationErrorCode.RESOURCE_NOT_FOUND, ApplicationErrorCategory.NOT_FOUND, message);
    }

    public NotFoundException(String message, Map<String, Object> metadata) {
        super(ApplicationErrorCode.RESOURCE_NOT_FOUND, ApplicationErrorCategory.NOT_FOUND, message, metadata);
    }
}
