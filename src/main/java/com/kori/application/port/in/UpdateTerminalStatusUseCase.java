package com.kori.application.port.in;

import com.kori.application.command.UpdateTerminalStatusCommand;
import com.kori.application.result.UpdateTerminalStatusResult;

public interface UpdateTerminalStatusUseCase {
    UpdateTerminalStatusResult execute(UpdateTerminalStatusCommand command);
}
