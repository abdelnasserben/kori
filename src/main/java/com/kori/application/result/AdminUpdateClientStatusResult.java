package com.kori.application.result;

import java.util.Objects;

public record AdminUpdateClientStatusResult(String clientId, String status) {

    public AdminUpdateClientStatusResult(String clientId, String status) {
        this.clientId = Objects.requireNonNull(clientId);
        this.status = Objects.requireNonNull(status);
    }
}
