package com.kori.application.port.in;

import com.kori.application.command.FailAgentPayoutCommand;
import com.kori.application.result.FinalizationResult;

public interface FailAgentPayoutUseCase {
    FinalizationResult execute(FailAgentPayoutCommand command);
}
