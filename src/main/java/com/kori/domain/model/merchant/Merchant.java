package com.kori.domain.model.merchant;

import com.kori.domain.common.InvalidStatusTransitionException;
import com.kori.domain.model.common.Status;

import java.time.Instant;
import java.util.Objects;

public final class Merchant {
    private final MerchantId id;
    private final MerchantCode code;
    private Status status;
    private final Instant createdAt;

    public Merchant(MerchantId id, MerchantCode code, Status status, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.code = Objects.requireNonNull(code, "code");
        this.status = Objects.requireNonNull(status, "code");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public MerchantId id() {
        return id;
    }

    public MerchantCode code() {
        return code;
    }

    public Status status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    // -----------------
    // Explicit status actions (admin-only at application layer)
    // -----------------

    public void suspend() {
        ensureNotClosed("suspend");
        if (status == Status.SUSPENDED) return; // idempotent
        status = Status.SUSPENDED;
    }

    public void activate() {
        ensureNotClosed("activate");
        if (status == Status.ACTIVE) return; // idempotent
        status = Status.ACTIVE;
    }

    public void close() {
        if (status == Status.CLOSED) return; // idempotent
        status = Status.CLOSED;
    }

    private void ensureNotClosed(String action) {
        if (status == Status.CLOSED) {
            throw new InvalidStatusTransitionException("Cannot " + action + " a CLOSED merchant");
        }
    }
}
