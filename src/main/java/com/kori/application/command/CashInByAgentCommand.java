package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.math.BigDecimal;

public record CashInByAgentCommand(
        String idempotencyKey,
        String idempotencyRequestHash,
        ActorContext actorContext,
        String clientPhoneNumber,
        BigDecimal amount
) {
}
