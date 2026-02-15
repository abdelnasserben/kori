package com.kori.domain.model.client;

import com.kori.domain.common.InvalidStatusTransitionException;
import com.kori.domain.model.common.Status;

import java.time.Instant;
import java.util.Objects;

public final class Client {
    private final ClientId id;
    private final ClientCode code;
    private final PhoneNumber phoneNumber;
    private Status status;
    private final Instant createdAt;

    public Client(ClientId id, ClientCode code, PhoneNumber phoneNumber, Status status, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.code = Objects.requireNonNull(code);
        this.phoneNumber = Objects.requireNonNull(phoneNumber);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public Client(ClientId id, String phoneNumber, Status status, Instant createdAt) {
        this(id, deriveLegacyCode(id), PhoneNumber.of(phoneNumber), status, createdAt);
    }

    public static Client activeNew(ClientId clientId, ClientCode code, PhoneNumber phoneNumber, Instant createdAt) {
        return new Client(clientId, code, phoneNumber, Status.ACTIVE, createdAt);
    }

    public ClientId id() {
        return id;
    }

    public ClientCode code() {
        return code;
    }

    public PhoneNumber phoneNumber() {
        return phoneNumber;
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
            throw new InvalidStatusTransitionException("Cannot " + action + " a CLOSED client");
        }
    }

    private static ClientCode deriveLegacyCode(ClientId id) {
        int numeric = Math.floorMod(id.value().hashCode(), 1_000_000);
        return ClientCode.of("C-" + String.format("%06d", numeric));
    }
}
