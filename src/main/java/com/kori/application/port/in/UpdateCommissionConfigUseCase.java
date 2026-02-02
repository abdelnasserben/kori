package com.kori.application.port.in;

import com.kori.application.command.UpdateCommissionConfigCommand;
import com.kori.application.result.UpdateCommissionConfigResult;

public interface UpdateCommissionConfigUseCase {
    UpdateCommissionConfigResult execute(UpdateCommissionConfigCommand command);
}
