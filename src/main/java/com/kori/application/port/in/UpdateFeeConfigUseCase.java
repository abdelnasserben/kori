package com.kori.application.port.in;

import com.kori.application.command.UpdateFeeConfigCommand;
import com.kori.application.result.UpdateFeeConfigResult;

public interface UpdateFeeConfigUseCase {
    UpdateFeeConfigResult execute(UpdateFeeConfigCommand command);
}
