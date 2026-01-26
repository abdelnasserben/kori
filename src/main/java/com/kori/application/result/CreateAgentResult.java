package com.kori.application.result;

import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.agent.AgentId;

import java.util.Objects;

public record CreateAgentResult(AgentId agentId, AgentCode agentCode) {
    public CreateAgentResult(AgentId agentId, AgentCode agentCode) {
        this.agentId = Objects.requireNonNull(agentId, "agentCode");
        this.agentCode = Objects.requireNonNull(agentCode, "agentCode");
    }
}
