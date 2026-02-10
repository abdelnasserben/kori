package com.kori.application.result;

public enum FinalizationResult {
    APPLIED,
    ALREADY_APPLIED;

    public boolean isAlreadyApplied() {
        return this == ALREADY_APPLIED;
    }
}

