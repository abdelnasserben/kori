package com.kori.application.command;

import java.util.Objects;

public record CreateMerchantCommand(String idempotencyKey) {
    public CreateMerchantCommand(String idempotencyKey) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
    }
}
