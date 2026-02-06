package com.kori.application.exception;

import java.util.Map;

public class BalanceMustBeZeroException extends ApplicationException {

    public BalanceMustBeZeroException(String message) {
        super(ApplicationErrorCode.BALANCE_MUST_BE_ZERO, ApplicationErrorCategory.CONFLICT, message);
    }

    public BalanceMustBeZeroException(String message, Map<String, Object> metadata) {
        super(ApplicationErrorCode.BALANCE_MUST_BE_ZERO, ApplicationErrorCategory.CONFLICT, message, metadata);
    }
}
