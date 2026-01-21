package com.kori.application.port.in;

import com.kori.application.command.GetBalanceCommand;
import com.kori.application.result.BalanceResult;

public interface GetBalanceUseCase {
    BalanceResult execute(GetBalanceCommand command);
}
