package com.kori.domain.model.client;

import com.kori.domain.common.InvalidStatusTransitionException;
import com.kori.domain.model.common.Status;

import java.util.Objects;

public final class Client {
    private final ClientId id;
    private final String phoneNumber;
    private Status status;

    public Client(ClientId id, String phoneNumber, Status status) {
        this.id = Objects.requireNonNull(id);
        this.phoneNumber = Objects.requireNonNull(phoneNumber);
        this.status = Objects.requireNonNull(status);
    }

    public static Client activeNew(ClientId clientId, String phoneNumber) {
        return new Client(clientId, phoneNumber, Status.ACTIVE);
    }

    public ClientId id() {
        return id;
    }

    public String phoneNumber() {
        return phoneNumber;
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
            throw new InvalidStatusTransitionException("Cannot " + action + " a CLOSED client");
        }
    }
}
