package com.kori.application.port.in;

import com.kori.application.command.AgentUpdateCardStatusCommand;
import com.kori.application.result.UpdateCardStatusResult;

public interface AgentUpdateCardStatusUseCase {
    UpdateCardStatusResult execute(AgentUpdateCardStatusCommand command);
}
