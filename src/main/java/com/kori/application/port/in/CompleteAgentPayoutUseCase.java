package com.kori.application.port.in;

import com.kori.application.command.CompleteAgentPayoutCommand;

public interface CompleteAgentPayoutUseCase {
    void execute(CompleteAgentPayoutCommand command);
}
