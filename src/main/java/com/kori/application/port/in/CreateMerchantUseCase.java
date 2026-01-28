package com.kori.application.port.in;

import com.kori.application.command.CreateMerchantCommand;
import com.kori.application.result.CreateMerchantResult;

public interface CreateMerchantUseCase {
    CreateMerchantResult execute(CreateMerchantCommand command);
}
