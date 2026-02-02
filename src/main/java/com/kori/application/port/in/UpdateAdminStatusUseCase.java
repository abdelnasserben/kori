package com.kori.application.port.in;

import com.kori.application.command.UpdateAdminStatusCommand;
import com.kori.application.result.UpdateAdminStatusResult;

public interface UpdateAdminStatusUseCase {
    UpdateAdminStatusResult execute(UpdateAdminStatusCommand command);
}
