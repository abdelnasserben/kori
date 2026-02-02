package com.kori.application.result;

public record UpdateAdminStatusResult(
        String adminId,
        String previousStatus,
        String newStatus
) {}
