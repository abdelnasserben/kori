package com.kori.application.usecase;

import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.GetCommissionConfigUseCase;
import com.kori.application.port.out.CommissionConfigPort;
import com.kori.application.result.CommissionConfigResult;
import com.kori.application.security.ActorContext;

public class GetCommissionConfigService implements GetCommissionConfigUseCase {

    private final AdminAccessService adminAccessService;
    private final CommissionConfigPort commissionConfigPort;

    public GetCommissionConfigService(AdminAccessService adminAccessService, CommissionConfigPort commissionConfigPort) {
        this.adminAccessService = adminAccessService;
        this.commissionConfigPort = commissionConfigPort;
    }

    @Override
    public CommissionConfigResult execute(ActorContext actorContext) {
        adminAccessService.requireActiveAdmin(actorContext, "read commission config");

        var config = commissionConfigPort.get().orElseThrow(() -> new NotFoundException("Commission config not found"));
        return new CommissionConfigResult(
                config.cardEnrollmentAgentCommission(),
                config.merchantWithdrawCommissionRate(),
                config.merchantWithdrawCommissionMin(),
                config.merchantWithdrawCommissionMax()
        );
    }
}
