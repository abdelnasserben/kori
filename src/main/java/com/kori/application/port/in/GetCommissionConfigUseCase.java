package com.kori.application.port.in;

import com.kori.application.result.CommissionConfigResult;
import com.kori.application.security.ActorContext;

public interface GetCommissionConfigUseCase {
    CommissionConfigResult execute(ActorContext actorContext);
}

