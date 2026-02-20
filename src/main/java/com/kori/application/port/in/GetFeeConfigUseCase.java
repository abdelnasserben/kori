package com.kori.application.port.in;

import com.kori.application.result.FeeConfigResult;
import com.kori.application.security.ActorContext;

public interface GetFeeConfigUseCase {
    FeeConfigResult execute(ActorContext actorContext);
}

