package com.kori.application.port.in;

import com.kori.application.command.UpdateCommissionConfigCommand;
import com.kori.application.result.CommissionConfigResult;

public interface UpdateCommissionConfigUseCase {
    CommissionConfigResult execute(UpdateCommissionConfigCommand command);
}
