package com.kori.domain.model.merchant;

import com.kori.domain.model.common.Status;

import java.util.Objects;

public final class Merchant {
    private final MerchantId id;
    private Status status;

    public Merchant(MerchantId id, Status status) {
        this.id = Objects.requireNonNull(id);
        this.status = Objects.requireNonNull(status);
    }

    public MerchantId id() {
        return id;
    }

    public Status status() {
        return status;
    }
}
