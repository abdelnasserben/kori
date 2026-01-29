package com.kori.application.result;

public record UpdateMerchantStatusResult(
        String merchantCode,
        String previousStatus,
        String newStatus
) {}
