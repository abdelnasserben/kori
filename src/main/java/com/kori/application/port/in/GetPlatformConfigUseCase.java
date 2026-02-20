package com.kori.application.port.in;

import com.kori.application.result.PlatformConfigResult;
import com.kori.application.security.ActorContext;

public interface GetPlatformConfigUseCase {
    PlatformConfigResult execute(ActorContext actorContext);
}
