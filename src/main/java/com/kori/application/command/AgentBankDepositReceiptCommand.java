package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.math.BigDecimal;
import java.util.Objects;

public record AgentBankDepositReceiptCommand(
        String idempotencyKey,
        String idempotencyRequestHash,
        ActorContext actorContext,
        String agentCode,
        BigDecimal amount
) {

    public AgentBankDepositReceiptCommand(
            String idempotencyKey,
            String idempotencyRequestHash,
            ActorContext actorContext,
            String agentCode,
            BigDecimal amount
    ) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.idempotencyRequestHash = Objects.requireNonNull(idempotencyRequestHash);
        this.actorContext = Objects.requireNonNull(actorContext);
        this.agentCode = Objects.requireNonNull(agentCode);
        this.amount = Objects.requireNonNull(amount);
    }
}
