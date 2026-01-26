package com.kori.application.exception;

public class InvalidPinFormatException extends RuntimeException {
    public InvalidPinFormatException(String message) {
        super(message);
    }
}
