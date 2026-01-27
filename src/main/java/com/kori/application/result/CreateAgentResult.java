package com.kori.application.result;

import java.util.Objects;

public record CreateAgentResult(String agentId, String agentCode) {
    public CreateAgentResult(String agentId, String agentCode) {
        this.agentId = Objects.requireNonNull(agentId, "agentCode");
        this.agentCode = Objects.requireNonNull(agentCode, "agentCode");
    }
}
