package com.kori.application.port.in;

import com.kori.application.command.UpdatePlatformConfigCommand;
import com.kori.application.result.PlatformConfigResult;

public interface UpdatePlatformConfigUseCase {
    PlatformConfigResult execute(UpdatePlatformConfigCommand command);
}
