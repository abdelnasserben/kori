package com.kori.domain.model.terminal;

import com.kori.domain.common.InvalidStatusTransitionException;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.MerchantId;

import java.time.Instant;
import java.util.Objects;

public final class Terminal {
    private final TerminalId id;
    private final MerchantId merchantId;
    private Status status;
    private final Instant createdAt;

    public Terminal(TerminalId id, MerchantId merchantId, Status status, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.merchantId = Objects.requireNonNull(merchantId);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public TerminalId id() {
        return id;
    }

    public MerchantId merchantId() {
        return merchantId;
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
            throw new InvalidStatusTransitionException("Cannot " + action + " a CLOSED terminal");
        }
    }
}
