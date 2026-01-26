package com.kori.application.result;

import com.kori.domain.model.merchant.MerchantCode;
import com.kori.domain.model.merchant.MerchantId;

import java.util.Objects;

public record CreateMerchantResult(
        MerchantId merchantId,
        MerchantCode code
) {
    public CreateMerchantResult {
        Objects.requireNonNull(merchantId);
        Objects.requireNonNull(code);
    }
}
