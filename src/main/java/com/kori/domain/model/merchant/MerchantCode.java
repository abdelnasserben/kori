package com.kori.domain.model.merchant;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.regex.Pattern;

public final class MerchantCode {

    private static final String PREFIX = "M-";
    private static final Pattern FORMAT =
            Pattern.compile("^M-[0-9]{6}$");

    private static final SecureRandom RANDOM = new SecureRandom();

    private final String value;

    private MerchantCode(String value) {
        this.value = value;
    }


    public static MerchantCode of(String raw) {
        Objects.requireNonNull(raw, "merchantCode");
        String normalized = raw.trim().toUpperCase();

        if (!FORMAT.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Invalid merchantCode format. Expected M-XXXXXX"
            );
        }

        return new MerchantCode(normalized);
    }

    /**
     * Génère un code marchand valide.
     * L'unicité doit être vérifiée par l'application.
     */
    public static MerchantCode generate() {
        int number = RANDOM.nextInt(1_000_000); // 000000 → 999999
        String formatted = String.format("%s%06d", PREFIX, number);
        return new MerchantCode(formatted);
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
