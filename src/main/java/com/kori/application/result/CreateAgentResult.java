package com.kori.application.result;

import java.util.Objects;

public record CreateAgentResult(String agentId, String agentCode, String displayName) {
    public CreateAgentResult {
        Objects.requireNonNull(agentId, "agentId");
        Objects.requireNonNull(agentCode, "agentCode");
    }
}
