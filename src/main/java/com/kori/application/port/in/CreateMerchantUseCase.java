package com.kori.application.port.in;

import com.kori.application.command.CreateMerchantCommand;
import com.kori.application.result.CreateMerchantResult;
import com.kori.application.security.ActorContext;

public interface CreateMerchantUseCase {
    CreateMerchantResult execute(
            CreateMerchantCommand command,
            ActorContext actorContext
    );
}
