package com.kori.domain.model.terminal;

import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.MerchantId;

import java.time.Instant;
import java.util.Objects;

public record Terminal(TerminalId id, MerchantId merchantId, Status status, Instant createdAt) {
    public Terminal(TerminalId id, MerchantId merchantId, Status status, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.merchantId = Objects.requireNonNull(merchantId);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
    }
}
