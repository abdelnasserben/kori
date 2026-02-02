package com.kori.application.port.in;

import com.kori.application.command.FailAgentPayoutCommand;
import com.kori.application.result.AgentPayoutResult;

public interface FailAgentPayoutUseCase {
    AgentPayoutResult execute(FailAgentPayoutCommand command);
}
