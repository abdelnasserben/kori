package com.kori.application.port.in;

import com.kori.application.command.AdminUpdateCardStatusCommand;
import com.kori.application.result.AdminUpdateCardStatusResult;

public interface AdminUpdateCardStatusUseCase {
    AdminUpdateCardStatusResult execute(AdminUpdateCardStatusCommand command);
}
