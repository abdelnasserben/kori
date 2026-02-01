package com.kori.domain.model.common;

import com.kori.domain.common.DomainException;

public enum Status {
    ACTIVE,
    SUSPENDED,
    CLOSED;

    public static Status parseStatus(String value) {
        try {
            return Status.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new DomainException("Invalid target status: " + value);
        }
    }
}
