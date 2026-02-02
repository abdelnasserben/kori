package com.kori.application.result;

import java.util.Objects;

public record CreateAdminResult(String adminId) {
    public CreateAdminResult(String adminId) {
        this.adminId = Objects.requireNonNull(adminId, "adminId");
    }
}
