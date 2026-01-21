package com.kori.domain.model.client;

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

    public static Client activeNew(String phoneNumber) {
        return new Client(ClientId.newId(), phoneNumber, Status.ACTIVE);
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
}
