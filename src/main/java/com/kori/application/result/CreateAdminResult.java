package com.kori.application.result;

import java.util.Objects;

public record CreateAdminResult(String adminId, String adminUsername, String displayName) {
    public CreateAdminResult {
        Objects.requireNonNull(adminId, "adminId");
        Objects.requireNonNull(adminUsername, "adminUsername");
    }
}
