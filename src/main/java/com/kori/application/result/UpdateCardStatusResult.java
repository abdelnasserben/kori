package com.kori.application.result;

public record UpdateCardStatusResult(
        String cardUid,
        String previousStatus,
        String newStatus
) {}
