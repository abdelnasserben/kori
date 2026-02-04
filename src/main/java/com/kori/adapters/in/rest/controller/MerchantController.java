package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.IdempotencyRequestHasher;
import com.kori.adapters.in.rest.IdempotentOperation;
import com.kori.adapters.in.rest.RestActorContextResolver;
import com.kori.adapters.in.rest.dto.Requests.UpdateStatusRequest;
import com.kori.adapters.in.rest.dto.Responses.CreateMerchantResponse;
import com.kori.adapters.in.rest.dto.Responses.UpdateStatusResponse;
import com.kori.application.command.CreateMerchantCommand;
import com.kori.application.command.UpdateMerchantStatusCommand;
import com.kori.application.port.in.CreateMerchantUseCase;
import com.kori.application.port.in.UpdateMerchantStatusUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.MERCHANTS)
@Tag(name = "Merchants")
public class MerchantController {

    private final CreateMerchantUseCase createMerchantUseCase;
    private final UpdateMerchantStatusUseCase updateMerchantStatusUseCase;
    private final IdempotencyRequestHasher idempotencyRequestHasher;

    public MerchantController(CreateMerchantUseCase createMerchantUseCase,
                              UpdateMerchantStatusUseCase updateMerchantStatusUseCase, IdempotencyRequestHasher idempotencyRequestHasher) {
        this.createMerchantUseCase = createMerchantUseCase;
        this.updateMerchantStatusUseCase = updateMerchantStatusUseCase;
        this.idempotencyRequestHasher = idempotencyRequestHasher;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create merchant")
    @IdempotentOperation
    public CreateMerchantResponse createMerchant(
            @RequestHeader(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = createMerchantUseCase.execute(
                new CreateMerchantCommand(idempotencyKey, idempotencyRequestHasher.hashPayload(null), actorContext)
        );
        return new CreateMerchantResponse(result.merchantId(), result.code());
    }

    @PatchMapping("/{merchantCode}/status")
    @Operation(summary = "Update merchant status")
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
