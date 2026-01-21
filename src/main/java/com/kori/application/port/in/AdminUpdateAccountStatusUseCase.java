package com.kori.application.port.in;

import com.kori.application.command.AdminUpdateAccountStatusCommand;
import com.kori.application.result.AdminUpdateAccountStatusResult;

public interface AdminUpdateAccountStatusUseCase {
    AdminUpdateAccountStatusResult execute(AdminUpdateAccountStatusCommand command);
}