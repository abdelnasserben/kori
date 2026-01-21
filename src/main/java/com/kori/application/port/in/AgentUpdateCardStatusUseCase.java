package com.kori.application.port.in;

import com.kori.application.command.AgentUpdateCardStatusCommand;
import com.kori.application.result.AgentUpdateCardStatusResult;

public interface AgentUpdateCardStatusUseCase {
    AgentUpdateCardStatusResult execute(AgentUpdateCardStatusCommand command);
}
