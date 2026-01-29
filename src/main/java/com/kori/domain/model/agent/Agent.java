package com.kori.domain.model.agent;

import com.kori.domain.common.InvalidStatusTransitionException;
import com.kori.domain.model.common.Status;

import java.time.Instant;
import java.util.Objects;

public final class Agent {

    private final AgentId id;
    private final AgentCode code;
    private final Instant createdAt;

    private Status status;

    public Agent(AgentId id, AgentCode code, Instant createdAt, Status status) {
        this.id = Objects.requireNonNull(id, "id");
        this.code = Objects.requireNonNull(code, "code");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.status = Objects.requireNonNull(status, "status");
    }

    public static Agent activeNew(AgentId id, AgentCode code, Instant createdAt) {
        return new Agent(id, code, createdAt, Status.ACTIVE);
    }

    public AgentId id() {
        return id;
    }

    public AgentCode code() {
        return code;
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
            throw new InvalidStatusTransitionException("Cannot " + action + " a CLOSED agent");
        }
    }
}
