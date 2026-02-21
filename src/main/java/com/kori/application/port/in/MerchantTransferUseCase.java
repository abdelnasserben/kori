package com.kori.application.port.in;

import com.kori.application.command.MerchantTransferCommand;
import com.kori.application.result.MerchantTransferResult;

public interface MerchantTransferUseCase {
    MerchantTransferResult execute(MerchantTransferCommand command);
}
