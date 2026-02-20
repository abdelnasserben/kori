package com.kori.application.port.in;

import com.kori.application.command.ClientTransferCommand;
import com.kori.application.result.ClientTransferResult;

public interface ClientTransferUseCase {
    ClientTransferResult execute(ClientTransferCommand command);
}
