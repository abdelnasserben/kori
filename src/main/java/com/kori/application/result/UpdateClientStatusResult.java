package com.kori.application.result;

public record UpdateClientStatusResult(
        String clientCode,
        String previousStatus,
        String newStatus
) {}
