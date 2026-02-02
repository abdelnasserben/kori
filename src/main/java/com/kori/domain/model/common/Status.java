package com.kori.domain.model.common;

import com.kori.domain.common.DomainErrorCategory;
import com.kori.domain.common.DomainErrorCode;
import com.kori.domain.common.DomainException;

public enum Status {
    ACTIVE,
    SUSPENDED,
    CLOSED;

    public static Status parseStatus(String value) {
        try {
            return Status.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new DomainException(
                    DomainErrorCode.INVALID_STATUS_VALUE,
                    DomainErrorCategory.VALIDATION,
                    "Invalid target status: " + value
            );
        }
    }
}
