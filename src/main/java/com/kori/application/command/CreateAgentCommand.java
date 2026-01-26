package com.kori.application.command;

import java.util.Objects;

public record CreateAgentCommand(String idempotencyKey) {
    public CreateAgentCommand(String idempotencyKey) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
    }
}
