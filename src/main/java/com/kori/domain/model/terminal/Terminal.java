package com.kori.domain.model.terminal;

import com.kori.domain.model.merchant.MerchantId;

import java.util.Objects;

public final class Terminal {
    private final TerminalId id;
    private final MerchantId merchantId;

    public Terminal(TerminalId id, MerchantId merchantId) {
        this.id = Objects.requireNonNull(id);
        this.merchantId = Objects.requireNonNull(merchantId);
    }

    public TerminalId id() {
        return id;
    }

    public MerchantId merchantId() {
        return merchantId;
    }
}
