package com.kori.application.port.in;

import com.kori.application.command.CompleteAgentPayoutCommand;
import com.kori.application.result.FinalizationResult;

public interface CompleteAgentPayoutUseCase {
    FinalizationResult execute(CompleteAgentPayoutCommand command);
}
