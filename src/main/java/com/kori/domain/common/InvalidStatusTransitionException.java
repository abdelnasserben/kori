package com.kori.domain.common;

public final class InvalidStatusTransitionException extends DomainException {
    public InvalidStatusTransitionException(String message) {
        super(DomainErrorCode.INVALID_STATUS_TRANSITION, DomainErrorCategory.INVARIANT, message);
    }
}
