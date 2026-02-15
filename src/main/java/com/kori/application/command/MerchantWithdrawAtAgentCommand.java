package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.math.BigDecimal;
import java.util.Objects;

public record MerchantWithdrawAtAgentCommand(
        String idempotencyKey,
        String idempotencyRequestHash,
        ActorContext actorContext,
        String merchantCode,
        BigDecimal amount) {
    public MerchantWithdrawAtAgentCommand(
            String idempotencyKey,
            String idempotencyRequestHash,
            ActorContext actorContext,
            String merchantCode,
            BigDecimal amount) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.idempotencyRequestHash = Objects.requireNonNull(idempotencyRequestHash);
        this.actorContext = Objects.requireNonNull(actorContext);
        this.merchantCode = Objects.requireNonNull(merchantCode);
        this.amount = Objects.requireNonNull(amount);
    }
}
