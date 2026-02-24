package com.kori.application.utils;

@FunctionalInterface
public interface PinFailureRecorder {
    void record(String cardUid, int maxAttempts);
}
