package com.kori.application.result;

import java.util.Objects;

public record CreateMerchantResult(
        String merchantId,
        String code
) {
    public CreateMerchantResult {
        Objects.requireNonNull(merchantId);
        Objects.requireNonNull(code);
    }
}
