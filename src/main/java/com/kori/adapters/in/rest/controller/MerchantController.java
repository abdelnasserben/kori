package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.RestActorContextResolver;
import com.kori.adapters.in.rest.dto.Requests.UpdateStatusRequest;
import com.kori.adapters.in.rest.dto.Responses.CreateMerchantResponse;
import com.kori.adapters.in.rest.dto.Responses.UpdateStatusResponse;
import com.kori.application.command.CreateMerchantCommand;
import com.kori.application.command.UpdateMerchantStatusCommand;
import com.kori.application.port.in.CreateMerchantUseCase;
import com.kori.application.port.in.UpdateMerchantStatusUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final CreateMerchantUseCase createMerchantUseCase;
    private final UpdateMerchantStatusUseCase updateMerchantStatusUseCase;

    public MerchantController(CreateMerchantUseCase createMerchantUseCase,
                             UpdateMerchantStatusUseCase updateMerchantStatusUseCase) {
        this.createMerchantUseCase = createMerchantUseCase;
        this.updateMerchantStatusUseCase = updateMerchantStatusUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateMerchantResponse createMerchant(
            @RequestHeader(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = createMerchantUseCase.execute(new CreateMerchantCommand(idempotencyKey, actorContext));
        return new CreateMerchantResponse(result.merchantId(), result.code());
    }

    @PatchMapping("/{merchantCode}/status")
    public UpdateStatusResponse updateMerchantStatus(
            @PathVariable String merchantCode,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = updateMerchantStatusUseCase.execute(
                new UpdateMerchantStatusCommand(actorContext, merchantCode, request.targetStatus(), request.reason())
        );
        return new UpdateStatusResponse(result.merchantCode(), result.previousStatus(), result.newStatus());
    }
}
