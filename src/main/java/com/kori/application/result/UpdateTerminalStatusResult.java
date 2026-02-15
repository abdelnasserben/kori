package com.kori.application.result;

public record UpdateTerminalStatusResult(
        String terminalUid,
        String previousStatus,
        String newStatus
) {}
