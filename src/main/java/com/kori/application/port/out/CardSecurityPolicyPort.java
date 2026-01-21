package com.kori.application.port.out;

/**
 * Security-related dynamic rules (admin-configurable in Phase 2).
 * Not financial, but kept as a policy for consistency and testability.
 */
@FunctionalInterface
public interface CardSecurityPolicyPort {
    int maxFailedPinAttempts();
}
