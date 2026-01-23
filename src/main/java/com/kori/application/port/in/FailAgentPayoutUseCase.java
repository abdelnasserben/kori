package com.kori.application.port.in;

import com.kori.application.command.FailAgentPayoutCommand;

public interface FailAgentPayoutUseCase {
    void execute(FailAgentPayoutCommand command);
}
