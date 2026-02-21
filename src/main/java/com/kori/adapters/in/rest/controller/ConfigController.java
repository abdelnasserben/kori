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
import com.kori.application.port.in.*;
import com.kori.application.security.ActorContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.CONFIG)
@Tag(name = "Config")
public class ConfigController {

    private final UpdateFeeConfigUseCase updateFeeConfigUseCase;
    private final UpdateCommissionConfigUseCase updateCommissionConfigUseCase;
    private final UpdatePlatformConfigUseCase updatePlatformConfigUseCase;
    private final GetFeeConfigUseCase getFeeConfigUseCase;
    private final GetCommissionConfigUseCase getCommissionConfigUseCase;
    private final GetPlatformConfigUseCase getPlatformConfigUseCase;

    public ConfigController(UpdateFeeConfigUseCase updateFeeConfigUseCase,
                            UpdateCommissionConfigUseCase updateCommissionConfigUseCase,
                            UpdatePlatformConfigUseCase updatePlatformConfigUseCase,
                            GetFeeConfigUseCase getFeeConfigUseCase,
                            GetCommissionConfigUseCase getCommissionConfigUseCase,
                            GetPlatformConfigUseCase getPlatformConfigUseCase) {
        this.updateFeeConfigUseCase = updateFeeConfigUseCase;
        this.updateCommissionConfigUseCase = updateCommissionConfigUseCase;
        this.updatePlatformConfigUseCase = updatePlatformConfigUseCase;
        this.getFeeConfigUseCase = getFeeConfigUseCase;
        this.getCommissionConfigUseCase = getCommissionConfigUseCase;
        this.getPlatformConfigUseCase = getPlatformConfigUseCase;
    }

    @GetMapping("/fees")
    @Operation(summary = "Get fees")
    public UpdateFeeConfigResponse getFees(ActorContext actorContext) {
        var result = getFeeConfigUseCase.execute(actorContext);
        return new UpdateFeeConfigResponse(
                result.cardEnrollmentPrice(),
                result.cardPaymentFeeRate(),
                result.cardPaymentFeeMin(),
                result.cardPaymentFeeMax(),
                result.merchantWithdrawFeeRate(),
                result.merchantWithdrawFeeMin(),
                result.merchantWithdrawFeeMax(),
                result.clientTransferFeeRate(),
                result.clientTransferFeeMin(),
                result.clientTransferFeeMax(),
                result.merchantTransferFeeRate(),
                result.merchantTransferFeeMin(),
                result.merchantTransferFeeMax(),
                result.cardPaymentFeeRefundable(),
                result.merchantWithdrawFeeRefundable(),
                result.cardEnrollmentPriceRefundable()
        );
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
                request.clientTransferFeeRate(),
                request.clientTransferFeeMin(),
                request.clientTransferFeeMax(),
                request.merchantTransferFeeRate(),
                request.merchantTransferFeeMin(),
                request.merchantTransferFeeMax(),
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
                result.clientTransferFeeRate(),
                result.clientTransferFeeMin(),
                result.clientTransferFeeMax(),
                result.merchantTransferFeeRate(),
                result.merchantTransferFeeMin(),
                result.merchantTransferFeeMax(),
                result.cardPaymentFeeRefundable(),
                result.merchantWithdrawFeeRefundable(),
                result.cardEnrollmentPriceRefundable()
        );
    }

    @GetMapping("/commissions")
    @Operation(summary = "Get commissions")
    public UpdateCommissionConfigResponse getCommissions(ActorContext actorContext) {
        var result = getCommissionConfigUseCase.execute(actorContext);
        return new UpdateCommissionConfigResponse(
                result.cardEnrollmentAgentCommission(),
                result.merchantWithdrawCommissionRate(),
                result.merchantWithdrawCommissionMin(),
                result.merchantWithdrawCommissionMax()
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

    @GetMapping("/platform")
    @Operation(summary = "Get platform config")
    public UpdatePlatformConfigResponse getPlatformConfig(ActorContext actorContext) {
        var result = getPlatformConfigUseCase.execute(actorContext);
        return new UpdatePlatformConfigResponse(
                result.agentCashLimitGlobal(),
                result.clientTransferMinPerTransaction(),
                result.clientTransferMaxPerTransaction(),
                result.clientTransferDailyMax(),
                result.merchantTransferMinPerTransaction(),
                result.merchantTransferMaxPerTransaction(),
                result.merchantTransferDailyMax(),
                result.merchantWithdrawMinPerTransaction()
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
                request.clientTransferMinPerTransaction(),
                request.clientTransferMaxPerTransaction(),
                request.clientTransferDailyMax(),
                request.merchantTransferMinPerTransaction(),
                request.merchantTransferMaxPerTransaction(),
                request.merchantTransferDailyMax(),
                request.merchantWithdrawMinPerTransaction(),
                request.reason()
        ));
        return new UpdatePlatformConfigResponse(
                result.agentCashLimitGlobal(),
                result.clientTransferMinPerTransaction(),
                result.clientTransferMaxPerTransaction(),
                result.clientTransferDailyMax(),
                result.merchantTransferMinPerTransaction(),
                result.merchantTransferMaxPerTransaction(),
                result.merchantTransferDailyMax(),
                result.merchantWithdrawMinPerTransaction()
        );
    }
}
