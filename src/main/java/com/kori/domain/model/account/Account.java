package com.kori.domain.model.account;

import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Status;

import java.util.Objects;

public final class Account {
    private final AccountId id;
    private final ClientId clientId;
    private Status status;

    public Account(AccountId id, ClientId clientId, Status status) {
        this.id = Objects.requireNonNull(id);
        this.clientId = Objects.requireNonNull(clientId);
        this.status = Objects.requireNonNull(status);
    }

    public static Account activeNew(ClientId clientId) {
        return new Account(AccountId.newId(), clientId, Status.ACTIVE);
    }

    public AccountId id() {
        return id;
    }

    public ClientId clientId() {
        return clientId;
    }

    public Status status() {
        return status;
    }
}
