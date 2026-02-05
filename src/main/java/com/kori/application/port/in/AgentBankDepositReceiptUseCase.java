package com.kori.application.port.in;

import com.kori.application.command.AgentBankDepositReceiptCommand;
import com.kori.application.result.AgentBankDepositReceiptResult;

public interface AgentBankDepositReceiptUseCase {
    AgentBankDepositReceiptResult execute(AgentBankDepositReceiptCommand command);
}
