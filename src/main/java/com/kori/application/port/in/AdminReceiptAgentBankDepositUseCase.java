package com.kori.application.port.in;

import com.kori.application.command.AdminReceiptAgentBankDepositCommand;
import com.kori.application.result.AdminReceiptAgentBankDepositResult;

public interface AdminReceiptAgentBankDepositUseCase {
    AdminReceiptAgentBankDepositResult execute(AdminReceiptAgentBankDepositCommand command);
}
