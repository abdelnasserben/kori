package com.kori.domain.model.merchant;

import com.kori.domain.common.InvalidStatusTransitionException;
import com.kori.domain.model.common.DisplayName;
import com.kori.domain.model.common.Status;

import java.time.Instant;
import java.util.Objects;

public final class Merchant {
    private final MerchantId id;
    private final MerchantCode code;
    private final DisplayName displayName;
    private Status status;
    private final Instant createdAt;

    public Merchant(MerchantId id, MerchantCode code, DisplayName displayName, Status status, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.code = Objects.requireNonNull(code, "code");
        this.displayName = displayName;
        this.status = Objects.requireNonNull(status, "code");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public Merchant(MerchantId id, MerchantCode code, Status status, Instant createdAt) {
        this(id, code, null, status, createdAt);
    }

    public MerchantId id() {
        return id;
    }

    public MerchantCode code() {
        return code;
    }

    public DisplayName displayName() {
        return displayName;
    }

    public String display() {
        return displayName != null ? displayName.value() : code.value();
    }

    public Status status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public void suspend() {
        ensureNotClosed("suspend");
        if (status == Status.SUSPENDED) return;
        status = Status.SUSPENDED;
    }

    public void activate() {
        ensureNotClosed("activate");
        if (status == Status.ACTIVE) return;
        status = Status.ACTIVE;
    }

    public void close() {
        if (status == Status.CLOSED) return;
        status = Status.CLOSED;
    }

    private void ensureNotClosed(String action) {
        if (status == Status.CLOSED) {
            throw new InvalidStatusTransitionException("Cannot " + action + " a CLOSED merchant");
        }
    }
}
