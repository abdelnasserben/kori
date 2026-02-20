package com.kori.application.usecase;

import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.GetFeeConfigUseCase;
import com.kori.application.port.out.FeeConfigPort;
import com.kori.application.result.FeeConfigResult;
import com.kori.application.security.ActorContext;

public class GetFeeConfigService implements GetFeeConfigUseCase {

    private final AdminAccessService adminAccessService;
    private final FeeConfigPort feeConfigPort;

    public GetFeeConfigService(AdminAccessService adminAccessService, FeeConfigPort feeConfigPort) {
        this.adminAccessService = adminAccessService;
        this.feeConfigPort = feeConfigPort;
    }

    @Override
    public FeeConfigResult execute(ActorContext actorContext) {
        adminAccessService.requireActiveAdmin(actorContext, "read fee config");

        var config = feeConfigPort.get().orElseThrow(() -> new NotFoundException("Fee config not found"));
        return new FeeConfigResult(
                config.cardEnrollmentPrice(),
                config.cardPaymentFeeRate(),
                config.cardPaymentFeeMin(),
                config.cardPaymentFeeMax(),
                config.merchantWithdrawFeeRate(),
                config.merchantWithdrawFeeMin(),
                config.merchantWithdrawFeeMax(),
                config.clientTransferFeeRate(),
                config.clientTransferFeeMin(),
                config.clientTransferFeeMax(),
                config.cardPaymentFeeRefundable(),
                config.merchantWithdrawFeeRefundable(),
                config.cardEnrollmentPriceRefundable()
        );
    }
}
