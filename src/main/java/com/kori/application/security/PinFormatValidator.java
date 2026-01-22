package com.kori.application.security;

import com.kori.application.exception.InvalidPinFormatException;

public final class PinFormatValidator {

    private PinFormatValidator() {}

    public static void validate(String pin) {
        if (pin == null || !pin.matches("\\d{4}")) {
            throw new InvalidPinFormatException("PIN must be exactly 4 digits");
        }
    }
}
