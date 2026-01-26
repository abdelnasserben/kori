package com.kori.application.port.in;

import com.kori.application.command.UpdateAccountProfileStatusCommand;
import com.kori.application.result.UpdateAccountProfileStatusResult;

public interface UpdateAccountProfileStatusUseCase {
    UpdateAccountProfileStatusResult execute(UpdateAccountProfileStatusCommand command);
}
