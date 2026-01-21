package com.kori.application.result;

import java.util.Objects;

public record AdminUpdateAccountStatusResult(String accountId, String status) {

    public AdminUpdateAccountStatusResult(String accountId, String status) {
        this.accountId = Objects.requireNonNull(accountId);
        this.status = Objects.requireNonNull(status);
    }
}