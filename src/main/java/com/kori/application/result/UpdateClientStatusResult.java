package com.kori.application.result;

public record UpdateClientStatusResult(
        String clientId,
        String previousStatus,
        String newStatus
) {}
