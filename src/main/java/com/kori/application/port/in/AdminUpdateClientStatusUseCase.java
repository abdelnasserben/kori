package com.kori.application.port.in;

import com.kori.application.command.AdminUpdateClientStatusCommand;
import com.kori.application.result.AdminUpdateClientStatusResult;

public interface AdminUpdateClientStatusUseCase {
    AdminUpdateClientStatusResult execute(AdminUpdateClientStatusCommand command);
}
