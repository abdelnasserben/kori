package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.math.BigDecimal;
import java.util.Objects;

public record MerchantTransferCommand(
        String idempotencyKey,
        String idempotencyRequestHash,
        ActorContext actorContext,
        String recipientMerchantCode,
        BigDecimal amount
) {
    public MerchantTransferCommand {
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(idempotencyRequestHash, "idempotencyRequestHash");
        Objects.requireNonNull(actorContext, "actorContext");
        Objects.requireNonNull(recipientMerchantCode, "recipientMerchantCode");
        Objects.requireNonNull(amount, "amount");
    }
}
