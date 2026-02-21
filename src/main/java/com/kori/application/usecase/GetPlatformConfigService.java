package com.kori.application.usecase;

import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.GetPlatformConfigUseCase;
import com.kori.application.port.out.PlatformConfigPort;
import com.kori.application.result.PlatformConfigResult;
import com.kori.application.security.ActorContext;

public class GetPlatformConfigService implements GetPlatformConfigUseCase {

    private final AdminAccessService adminAccessService;
    private final PlatformConfigPort platformConfigPort;

    public GetPlatformConfigService(AdminAccessService adminAccessService, PlatformConfigPort platformConfigPort) {
        this.adminAccessService = adminAccessService;
        this.platformConfigPort = platformConfigPort;
    }

    @Override
    public PlatformConfigResult execute(ActorContext actorContext) {
        adminAccessService.requireActiveAdmin(actorContext, "read platform config");

        var config = platformConfigPort.get().orElseThrow(() -> new NotFoundException("Platform config not found"));
        return new PlatformConfigResult(
                config.agentCashLimitGlobal(),
                config.clientTransferMaxPerTransaction(),
                config.clientTransferDailyMax(),
                config.merchantTransferMaxPerTransaction(),
                config.merchantTransferDailyMax()
        );
    }
}
