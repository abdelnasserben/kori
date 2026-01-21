package com.kori.domain.model.merchant;

import java.util.Objects;

public final class MerchantId {
    private final String value;

    private MerchantId(String value) {
        this.value = Objects.requireNonNull(value);
    }

    public static MerchantId of(String value) {
        return new MerchantId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MerchantId other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
