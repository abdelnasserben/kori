package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.dto.Requests.UpdateCommissionConfigRequest;
import com.kori.adapters.in.rest.dto.Requests.UpdateFeeConfigRequest;
import com.kori.adapters.in.rest.dto.Requests.UpdatePlatformConfigRequest;
import com.kori.adapters.in.rest.dto.Responses.UpdateCommissionConfigResponse;
import com.kori.adapters.in.rest.dto.Responses.UpdateFeeConfigResponse;
import com.kori.adapters.in.rest.dto.Responses.UpdatePlatformConfigResponse;
import com.kori.application.command.UpdateCommissionConfigCommand;
import com.kori.application.command.UpdateFeeConfigCommand;
import com.kori.application.command.UpdatePlatformConfigCommand;
import com.kori.application.port.in.UpdateCommissionConfigUseCase;
import com.kori.application.port.in.UpdateFeeConfigUseCase;
import com.kori.application.port.in.UpdatePlatformConfigUseCase;
import com.kori.application.security.ActorContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.CONFIG)
@Tag(name = "Config")
public class ConfigController {

    private final UpdateFeeConfigUseCase updateFeeConfigUseCase;
    private final UpdateCommissionConfigUseCase updateCommissionConfigUseCase;
    private final UpdatePlatformConfigUseCase updatePlatformConfigUseCase;

    public ConfigController(UpdateFeeConfigUseCase updateFeeConfigUseCase,
                            UpdateCommissionConfigUseCase updateCommissionConfigUseCase, UpdatePlatformConfigUseCase updatePlatformConfigUseCase) {
        this.updateFeeConfigUseCase = updateFeeConfigUseCase;
        this.updateCommissionConfigUseCase = updateCommissionConfigUseCase;
        this.updatePlatformConfigUseCase = updatePlatformConfigUseCase;
    }

    @PatchMapping("/fees")
    @Operation(summary = "Update fees")
    public UpdateFeeConfigResponse updateFees(
            ActorContext actorContext,
            @Valid @RequestBody UpdateFeeConfigRequest request
    ) {
        var result = updateFeeConfigUseCase.execute(new UpdateFeeConfigCommand(
                actorContext,
                request.cardEnrollmentPrice(),
                request.cardPaymentFeeRate(),
                request.cardPaymentFeeMin(),
                request.cardPaymentFeeMax(),
                request.merchantWithdrawFeeRate(),
                request.merchantWithdrawFeeMin(),
                request.merchantWithdrawFeeMax(),
                Boolean.TRUE.equals(request.cardPaymentFeeRefundable()),
                Boolean.TRUE.equals(request.merchantWithdrawFeeRefundable()),
                Boolean.TRUE.equals(request.cardEnrollmentPriceRefundable()),
                request.reason()
        ));
        return new UpdateFeeConfigResponse(
                result.cardEnrollmentPrice(),
                result.cardPaymentFeeRate(),
                result.cardPaymentFeeMin(),
                result.cardPaymentFeeMax(),
                result.merchantWithdrawFeeRate(),
                result.merchantWithdrawFeeMin(),
                result.merchantWithdrawFeeMax(),
                result.cardPaymentFeeRefundable(),
                result.merchantWithdrawFeeRefundable(),
                result.cardEnrollmentPriceRefundable()
        );
    }

    @PatchMapping("/commissions")
    @Operation(summary = "Update commissions")
    public UpdateCommissionConfigResponse updateCommissions(
            ActorContext actorContext,
            @Valid @RequestBody UpdateCommissionConfigRequest request
    ) {
        var result = updateCommissionConfigUseCase.execute(new UpdateCommissionConfigCommand(
                actorContext,
                request.cardEnrollmentAgentCommission(),
                request.merchantWithdrawCommissionRate(),
                request.merchantWithdrawCommissionMin(),
                request.merchantWithdrawCommissionMax(),
                request.reason()
        ));
        return new UpdateCommissionConfigResponse(
                result.cardEnrollmentAgentCommission(),
                result.merchantWithdrawCommissionRate(),
                result.merchantWithdrawCommissionMin(),
                result.merchantWithdrawCommissionMax()
        );
    }

    @PatchMapping("/platform")
    @Operation(summary = "Update platform config")
    public UpdatePlatformConfigResponse updatePlatformConfig(
            ActorContext actorContext,
            @Valid @RequestBody UpdatePlatformConfigRequest request
    ) {
        var result = updatePlatformConfigUseCase.execute(new UpdatePlatformConfigCommand(
                actorContext,
                request.agentCashLimitGlobal(),
                request.reason()
        ));
        return new UpdatePlatformConfigResponse(result.agentCashLimitGlobal());
    }
}
