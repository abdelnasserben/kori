package com.kori.application.port.in;

import com.kori.application.command.FailClientRefundCommand;
import com.kori.application.result.ClientRefundResult;

public interface FailClientRefundUseCase {
    ClientRefundResult execute(FailClientRefundCommand cmd);
}
