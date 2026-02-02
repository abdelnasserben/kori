package com.kori.domain.model.admin;

import com.kori.domain.common.InvalidStatusTransitionException;
import com.kori.domain.model.common.Status;

import java.time.Instant;
import java.util.Objects;

public final class Admin {
    private final AdminId id;
    private Status status;
    private final Instant createdAt;

    public Admin(AdminId id, Status status, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static Admin activeNew(AdminId id, Instant createdAt) {
        return new Admin(id, Status.ACTIVE, createdAt);
    }

    public AdminId id() {
        return id;
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
            throw new InvalidStatusTransitionException("Cannot " + action + " a CLOSED admin");
        }
    }
}
