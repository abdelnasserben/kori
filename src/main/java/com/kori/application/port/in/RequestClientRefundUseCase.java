package com.kori.application.port.in;

import com.kori.application.command.RequestClientRefundCommand;
import com.kori.application.result.ClientRefundResult;

public interface RequestClientRefundUseCase {
    ClientRefundResult execute(RequestClientRefundCommand cmd);
}
