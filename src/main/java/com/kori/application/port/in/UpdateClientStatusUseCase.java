package com.kori.application.port.in;

import com.kori.application.command.UpdateClientStatusCommand;
import com.kori.application.result.UpdateClientStatusResult;

public interface UpdateClientStatusUseCase {
    UpdateClientStatusResult execute(UpdateClientStatusCommand command);
}
