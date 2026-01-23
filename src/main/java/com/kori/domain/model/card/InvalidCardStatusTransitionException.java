package com.kori.domain.model.card;

public final class InvalidCardStatusTransitionException extends RuntimeException {
    public InvalidCardStatusTransitionException(String message) {
        super(message);
    }
}
