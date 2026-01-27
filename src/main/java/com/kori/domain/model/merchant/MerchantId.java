package com.kori.domain.model.merchant;

import java.util.Objects;
import java.util.UUID;

public record MerchantId(UUID value) {
    public MerchantId(UUID value) {
        this.value = Objects.requireNonNull(value);
    }

    public static MerchantId newId() {
        return new MerchantId(UUID.randomUUID());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MerchantId other)) return false;
        return value.equals(other.value);
    }

}
