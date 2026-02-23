package com.kori.domain.model.admin;

import com.kori.domain.common.InvalidStatusTransitionException;
import com.kori.domain.model.common.DisplayName;
import com.kori.domain.model.common.Status;

import java.time.Instant;
import java.util.Objects;

public final class Admin {
    private final AdminId id;
    private final AdminUsername username;
    private final DisplayName displayName;
    private Status status;
    private final Instant createdAt;

    public Admin(AdminId id, AdminUsername username, DisplayName displayName, Status status, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.username = Objects.requireNonNull(username, "username");
        this.displayName = displayName;
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public Admin(AdminId id, AdminUsername username, Status status, Instant createdAt) {
        this(id, username, null, status, createdAt);
    }

    public static Admin activeNew(AdminId id, AdminUsername username, DisplayName displayName, Instant createdAt) {
        return new Admin(id, username, displayName, Status.ACTIVE, createdAt);
    }

    public AdminId id() {
        return id;
    }

    public AdminUsername username() {
        return username;
    }

    public DisplayName displayName() {
        return displayName;
    }

    public String display() {
        return displayName != null ? displayName.value() : username.value();
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
            throw new InvalidStatusTransitionException("Cannot " + action + " a CLOSED admin");
        }
    }
}
