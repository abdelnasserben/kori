package com.kori.application.result;

import java.util.Objects;

public record CreateMerchantResult(String merchantId, String code, String displayName) {
    public CreateMerchantResult {
        Objects.requireNonNull(merchantId, "merchantId");
        Objects.requireNonNull(code, "code");
    }
}
