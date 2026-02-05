package com.kori.domain.model.clientrefund;

import java.util.Objects;
import java.util.UUID;

public record ClientRefundId(UUID value) {

    public ClientRefundId {
        Objects.requireNonNull(value);
    }

    public static ClientRefundId of(String value) {
        return new ClientRefundId(UUID.fromString(value));
    }
}
