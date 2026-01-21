package com.kori.application.port.in;

import com.kori.application.command.EnrollCardCommand;
import com.kori.application.result.EnrollCardResult;

public interface EnrollCardUseCase {
    EnrollCardResult execute(EnrollCardCommand command);
}
