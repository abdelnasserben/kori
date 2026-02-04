package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.RestActorContextResolver;
import com.kori.adapters.in.rest.dto.Requests.UpdateAccountProfileStatusRequest;
import com.kori.adapters.in.rest.dto.Responses.UpdateAccountProfileStatusResponse;
import com.kori.application.command.UpdateAccountProfileStatusCommand;
import com.kori.application.port.in.UpdateAccountProfileStatusUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.ACCOUNT_PROFILES)
@Tag(name = "Account Profiles")
public class AccountProfileController {

    private final UpdateAccountProfileStatusUseCase updateAccountProfileStatusUseCase;

    public AccountProfileController(UpdateAccountProfileStatusUseCase updateAccountProfileStatusUseCase) {
        this.updateAccountProfileStatusUseCase = updateAccountProfileStatusUseCase;
    }

    @PatchMapping("/status")
    @Operation(summary = "Update account profile status")
    public UpdateAccountProfileStatusResponse updateStatus(
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody UpdateAccountProfileStatusRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = updateAccountProfileStatusUseCase.execute(
                new UpdateAccountProfileStatusCommand(
                        actorContext,
                        request.accountType(),
                        request.ownerRef(),
                        request.targetStatus(),
                        request.reason()
                )
        );
        return new UpdateAccountProfileStatusResponse(
                result.accountType(),
                result.ownerRef(),
                result.previousStatus(),
                result.newStatus()
        );
    }
}
