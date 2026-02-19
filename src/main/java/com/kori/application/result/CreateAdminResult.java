package com.kori.application.result;

import java.util.Objects;

public record CreateAdminResult(String adminId, String adminUsername) {
    public CreateAdminResult(String adminId, String adminUsername) {
        this.adminId = Objects.requireNonNull(adminId, "adminId");
        this.adminUsername = Objects.requireNonNull(adminUsername, "adminUsername");
    }
}
