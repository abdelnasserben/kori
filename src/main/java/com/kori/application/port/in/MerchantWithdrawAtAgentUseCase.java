package com.kori.application.port.in;

import com.kori.application.command.MerchantWithdrawAtAgentCommand;
import com.kori.application.result.MerchantWithdrawAtAgentResult;

public interface MerchantWithdrawAtAgentUseCase {
    MerchantWithdrawAtAgentResult execute(MerchantWithdrawAtAgentCommand command);
}
