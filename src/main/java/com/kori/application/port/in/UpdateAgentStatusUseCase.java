package com.kori.application.port.in;

import com.kori.application.command.UpdateAgentStatusCommand;
import com.kori.application.result.UpdateAgentStatusResult;

public interface UpdateAgentStatusUseCase {
    UpdateAgentStatusResult execute(UpdateAgentStatusCommand command);
}
