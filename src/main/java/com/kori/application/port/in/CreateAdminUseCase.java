package com.kori.application.port.in;

import com.kori.application.command.CreateAdminCommand;
import com.kori.application.result.CreateAdminResult;

public interface CreateAdminUseCase {
    CreateAdminResult execute(CreateAdminCommand command);
}
