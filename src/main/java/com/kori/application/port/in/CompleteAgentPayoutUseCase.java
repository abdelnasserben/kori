package com.kori.application.port.in;

import com.kori.application.command.CompleteAgentPayoutCommand;
import com.kori.application.result.AgentPayoutResult;

public interface CompleteAgentPayoutUseCase {
    AgentPayoutResult execute(CompleteAgentPayoutCommand command);
}
