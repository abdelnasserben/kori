package com.kori.application.port.in;

import com.kori.application.command.RequestAgentPayoutCommand;
import com.kori.application.result.AgentPayoutResult;

public interface RequestAgentPayoutUseCase {
    AgentPayoutResult execute(RequestAgentPayoutCommand cmd);
}
