package com.kori.application.port.in;

import com.kori.application.command.UpdatePlatformConfigCommand;
import com.kori.application.result.UpdatePlatformConfigResult;

public interface UpdatePlatformConfigUseCase {
    UpdatePlatformConfigResult execute(UpdatePlatformConfigCommand command);
}
