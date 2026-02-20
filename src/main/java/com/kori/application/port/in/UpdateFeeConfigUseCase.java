package com.kori.application.port.in;

import com.kori.application.command.UpdateFeeConfigCommand;
import com.kori.application.result.FeeConfigResult;

public interface UpdateFeeConfigUseCase {
    FeeConfigResult execute(UpdateFeeConfigCommand command);
}
