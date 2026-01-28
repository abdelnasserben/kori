package com.kori.application.port.in;

import com.kori.application.command.CreateTerminalCommand;
import com.kori.application.result.CreateTerminalResult;

public interface CreateTerminalUseCase {
    CreateTerminalResult execute(CreateTerminalCommand command);
}
