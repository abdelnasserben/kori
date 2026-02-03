package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.RestActorContextResolver;
import com.kori.adapters.in.rest.dto.Requests.UpdateCommissionConfigRequest;
import com.kori.adapters.in.rest.dto.Requests.UpdateFeeConfigRequest;
import com.kori.adapters.in.rest.dto.Responses.UpdateCommissionConfigResponse;
import com.kori.adapters.in.rest.dto.Responses.UpdateFeeConfigResponse;
import com.kori.application.command.UpdateCommissionConfigCommand;
import com.kori.application.command.UpdateFeeConfigCommand;
import com.kori.application.port.in.UpdateCommissionConfigUseCase;
import com.kori.application.port.in.UpdateFeeConfigUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final UpdateFeeConfigUseCase updateFeeConfigUseCase;
    private final UpdateCommissionConfigUseCase updateCommissionConfigUseCase;

    public ConfigController(UpdateFeeConfigUseCase updateFeeConfigUseCase,
                            UpdateCommissionConfigUseCase updateCommissionConfigUseCase) {
        this.updateFeeConfigUseCase = updateFeeConfigUseCase;
        this.updateCommissionConfigUseCase = updateCommissionConfigUseCase;
    }

    @PatchMapping("/fees")
    public UpdateFeeConfigResponse updateFees(
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody UpdateFeeConfigRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = updateFeeConfigUseCase.execute(new UpdateFeeConfigCommand(
                actorContext,
                request.cardEnrollmentPrice(),
                request.cardPaymentFeeRate(),
                request.cardPaymentFeeMin(),
                request.cardPaymentFeeMax(),
                request.merchantWithdrawFeeRate(),
                request.merchantWithdrawFeeMin(),
                request.merchantWithdrawFeeMax(),
                request.reason()
        ));
        return new UpdateFeeConfigResponse(
                result.cardEnrollmentPrice(),
                result.cardPaymentFeeRate(),
                result.cardPaymentFeeMin(),
                result.cardPaymentFeeMax(),
                result.merchantWithdrawFeeRate(),
                result.merchantWithdrawFeeMin(),
                result.merchantWithdrawFeeMax()
        );
    }

    @PatchMapping("/commissions")
    public UpdateCommissionConfigResponse updateCommissions(
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody UpdateCommissionConfigRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
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
}
