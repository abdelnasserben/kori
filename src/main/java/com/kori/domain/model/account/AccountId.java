package com.kori.domain.model.account;

import java.util.Objects;
import java.util.UUID;

public final class AccountId {
    private final String value;

    private AccountId(String value) {
        this.value = Objects.requireNonNull(value);
    }

    public static AccountId newId() {
        return new AccountId(UUID.randomUUID().toString());
    }

    public static AccountId of(String value) {
        return new AccountId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountId other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
