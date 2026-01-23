package com.kori.domain.common;

public final class DomainPreconditions {
    private DomainPreconditions() {}

    public static <T> T notNull(T value, String message) {
        if (value == null) throw new DomainException(message);
        return value;
    }

    public static String notBlank(String value, String message) {
        if (value == null || value.isBlank()) throw new DomainException(message);
        return value;
    }

    public static void check(boolean condition, String message) {
        if (!condition) throw new DomainException(message);
    }
}
