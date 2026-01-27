package com.kori.domain.model.merchant;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Code marchand "terrain-friendly" : M-XXXXXX (6 digits).
 */
public final class MerchantCode {

    private static final Pattern FORMAT = Pattern.compile("^M-[0-9]{6}$");

    private final String value;

    private MerchantCode(String value) {
        this.value = value;
    }

    public static MerchantCode of(String raw) {
        Objects.requireNonNull(raw, "merchantCode");
        String normalized = raw.trim().toUpperCase();

        if (!FORMAT.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid merchantCode format. Expected M-XXXXXX");
        }

        return new MerchantCode(normalized);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MerchantCode other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
