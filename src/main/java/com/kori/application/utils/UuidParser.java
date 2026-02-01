package com.kori.application.utils;

import com.kori.application.exception.ApplicationException;

import java.util.UUID;

public final class UuidParser {
    private UuidParser() {}

    public static UUID parse(String value, String field) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ApplicationException("Invalid " + field + ": " + value);
        }
    }

}
