package com.kori.application.port.in;

import com.kori.application.command.AdminUpdateCardStatusCommand;
import com.kori.application.result.UpdateCardStatusResult;

public interface AdminUpdateCardStatusUseCase {
    UpdateCardStatusResult execute(AdminUpdateCardStatusCommand command);
}
