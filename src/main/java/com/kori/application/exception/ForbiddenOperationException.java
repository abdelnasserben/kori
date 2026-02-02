package com.kori.application.exception;

import java.util.Map;

public class ForbiddenOperationException extends ApplicationException {
    public ForbiddenOperationException(String message) {
        super(ApplicationErrorCode.FORBIDDEN_OPERATION, ApplicationErrorCategory.AUTHORIZATION, message);
    }

    public ForbiddenOperationException(String message, Map <String, Object> metadata) {
        super(ApplicationErrorCode.FORBIDDEN_OPERATION, ApplicationErrorCategory.AUTHORIZATION, message, metadata);
    }
}
