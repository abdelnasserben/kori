package com.kori.application.result;

public record UpdateAccountProfileStatusResult(
        String accountType,
        String ownerRef,
        String previousStatus,
        String newStatus
) {}
