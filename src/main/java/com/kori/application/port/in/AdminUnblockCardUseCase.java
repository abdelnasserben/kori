package com.kori.application.port.in;

import com.kori.application.command.AdminUnblockCardCommand;
import com.kori.application.result.AdminUnblockCardResult;

public interface AdminUnblockCardUseCase {
    AdminUnblockCardResult execute(AdminUnblockCardCommand command);
}
