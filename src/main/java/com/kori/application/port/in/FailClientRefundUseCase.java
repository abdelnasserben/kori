package com.kori.application.port.in;

import com.kori.application.command.FailClientRefundCommand;
import com.kori.application.result.FinalizationResult;

public interface FailClientRefundUseCase {
    FinalizationResult execute(FailClientRefundCommand cmd);
}
