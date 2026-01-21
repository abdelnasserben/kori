package com.kori.application.port.in;

import com.kori.application.command.AgentPayoutCommand;
import com.kori.application.result.AgentPayoutResult;

public interface AgentPayoutUseCase {
    AgentPayoutResult execute(AgentPayoutCommand command);
}
