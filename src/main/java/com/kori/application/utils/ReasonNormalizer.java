package com.kori.application.utils;

public final class ReasonNormalizer {

    private ReasonNormalizer() {}

    public static String normalize(String reason) {
        if (reason == null) {
            return "N/A";
        }
        String trimmed = reason.trim();
        return trimmed.isBlank() ? "N/A" : trimmed;
    }
}
