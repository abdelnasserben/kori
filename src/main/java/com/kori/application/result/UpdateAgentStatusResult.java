package com.kori.application.result;

public record UpdateAgentStatusResult(
        String agentCode,
        String previousStatus,
        String newStatus
) {}
