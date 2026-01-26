package com.kori.application.port.in;

import com.kori.application.command.AdminUnblockCardCommand;
import com.kori.application.result.UpdateCardStatusResult;

public interface AdminUnblockCardUseCase {
    UpdateCardStatusResult execute(AdminUnblockCardCommand command);
}
