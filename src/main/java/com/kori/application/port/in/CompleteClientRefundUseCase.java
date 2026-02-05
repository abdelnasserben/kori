package com.kori.application.port.in;

import com.kori.application.command.CompleteClientRefundCommand;
import com.kori.application.result.ClientRefundResult;

public interface CompleteClientRefundUseCase {
    ClientRefundResult execute(CompleteClientRefundCommand cmd);
}
