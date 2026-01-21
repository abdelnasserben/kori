package com.kori.application.port.in;

import com.kori.application.command.ReversalCommand;
import com.kori.application.result.ReversalResult;

public interface ReversalUseCase {
    ReversalResult execute(ReversalCommand command);
}
