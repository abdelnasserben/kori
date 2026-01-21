package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.math.BigDecimal;
import java.util.Objects;

public record MerchantWithdrawAtAgentCommand(String idempotencyKey, ActorContext actorContext, String merchantId,
                                             String agentId, BigDecimal amount) {
    public MerchantWithdrawAtAgentCommand(String idempotencyKey,
                                          ActorContext actorContext,
                                          String merchantId,
                                          String agentId,
                                          BigDecimal amount) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.actorContext = Objects.requireNonNull(actorContext);
        this.merchantId = Objects.requireNonNull(merchantId);
        this.agentId = Objects.requireNonNull(agentId);
        this.amount = Objects.requireNonNull(amount);
    }
}
