package com.kori.application.port.in;

import com.kori.application.command.CreateAgentCommand;
import com.kori.application.result.CreateAgentResult;
import com.kori.application.security.ActorContext;

public interface CreateAgentUseCase {
    CreateAgentResult execute(CreateAgentCommand command, ActorContext actorContext);
}
