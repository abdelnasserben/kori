package com.kori.application.exception;

import java.util.Map;

public class ActorContextAuthenticationException extends ApplicationException {

    public ActorContextAuthenticationException(String message) {
        super(
                ApplicationErrorCode.AUTHENTICATION_REQUIRED,
                ApplicationErrorCategory.AUTHORIZATION,
                message,
                Map.of()
        );
    }
}

