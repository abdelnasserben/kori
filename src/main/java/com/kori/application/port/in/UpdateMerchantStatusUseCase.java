package com.kori.application.port.in;

import com.kori.application.command.UpdateMerchantStatusCommand;
import com.kori.application.result.UpdateMerchantStatusResult;

public interface UpdateMerchantStatusUseCase {
    UpdateMerchantStatusResult execute(UpdateMerchantStatusCommand command);
}
