package com.kori.domain.model.merchant;

import com.kori.domain.model.common.Status;

import java.time.Instant;
import java.util.Objects;

public record Merchant(MerchantId id, MerchantCode code, Status status, Instant createdAt) {
    public Merchant(MerchantId id, MerchantCode code, Status status, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.code = Objects.requireNonNull(code, "code");
        this.status = Objects.requireNonNull(status, "code");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static String normalizeName(String raw) {
        if (raw == null) return null;
        return raw.trim().replaceAll("\\s+", " ");
    }
}
