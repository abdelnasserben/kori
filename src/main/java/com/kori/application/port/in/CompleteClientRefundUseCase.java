package com.kori.application.port.in;

import com.kori.application.command.CompleteClientRefundCommand;
import com.kori.application.result.FinalizationResult;

public interface CompleteClientRefundUseCase {
    FinalizationResult execute(CompleteClientRefundCommand cmd);
}
