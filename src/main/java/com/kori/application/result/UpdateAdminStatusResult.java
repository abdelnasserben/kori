package com.kori.application.result;

public record UpdateAdminStatusResult(
        String adminUsername,
        String previousStatus,
        String newStatus
) {}
