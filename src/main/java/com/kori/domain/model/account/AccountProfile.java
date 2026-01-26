package com.kori.domain.model.account;

import com.kori.domain.common.InvalidStatusTransitionException;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.common.Status;

import java.time.Instant;
import java.util.Objects;

public final class AccountProfile {
    private final LedgerAccountRef account;
    private final Instant createdAt;
    private Status status;

    public AccountProfile(LedgerAccountRef account, Instant createdAt, Status status) {
        this.account = Objects.requireNonNull(account, "accountRef");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.status = Objects.requireNonNull(status, "status");
    }

    public static AccountProfile activeNew(LedgerAccountRef account, Instant createdAt) {
        return new AccountProfile(account, createdAt, Status.ACTIVE);
    }

    public LedgerAccountRef account() {
        return account;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Status status() {
        return status;
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
            throw new InvalidStatusTransitionException("Cannot " + action + " a CLOSED accountProfile");
        }
    }
}
