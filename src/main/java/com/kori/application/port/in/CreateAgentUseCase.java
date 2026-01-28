package com.kori.application.port.in;

import com.kori.application.command.CreateAgentCommand;
import com.kori.application.result.CreateAgentResult;

public interface CreateAgentUseCase {
    CreateAgentResult execute(CreateAgentCommand command);
}
