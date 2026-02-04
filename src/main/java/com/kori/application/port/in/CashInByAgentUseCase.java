package com.kori.application.port.in;

import com.kori.application.command.CashInByAgentCommand;
import com.kori.application.result.CashInByAgentResult;

public interface CashInByAgentUseCase {
    CashInByAgentResult execute(CashInByAgentCommand command);
}
