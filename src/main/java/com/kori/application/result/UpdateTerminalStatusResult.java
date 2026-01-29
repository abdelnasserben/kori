package com.kori.application.result;

public record UpdateTerminalStatusResult(
        String terminalId,
        String previousStatus,
        String newStatus
) {}
