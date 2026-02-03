package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.RestActorContextResolver;
import com.kori.adapters.in.rest.dto.Requests.UpdateStatusRequest;
import com.kori.adapters.in.rest.dto.Responses.CreateAdminResponse;
import com.kori.adapters.in.rest.dto.Responses.UpdateStatusResponse;
import com.kori.application.command.CreateAdminCommand;
import com.kori.application.command.UpdateAdminStatusCommand;
import com.kori.application.port.in.CreateAdminUseCase;
import com.kori.application.port.in.UpdateAdminStatusUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admins")
public class AdminController {

    private final CreateAdminUseCase createAdminUseCase;
    private final UpdateAdminStatusUseCase updateAdminStatusUseCase;

    public AdminController(CreateAdminUseCase createAdminUseCase, UpdateAdminStatusUseCase updateAdminStatusUseCase) {
        this.createAdminUseCase = createAdminUseCase;
        this.updateAdminStatusUseCase = updateAdminStatusUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateAdminResponse createAdmin(
            @RequestHeader(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = createAdminUseCase.execute(new CreateAdminCommand(idempotencyKey, actorContext));
        return new CreateAdminResponse(result.adminId());
    }

    @PatchMapping("/{adminId}/status")
    public UpdateStatusResponse updateAdminStatus(
            @PathVariable String adminId,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = updateAdminStatusUseCase.execute(
                new UpdateAdminStatusCommand(actorContext, adminId, request.targetStatus(), request.reason())
        );
        return new UpdateStatusResponse(result.adminId(), result.previousStatus(), result.newStatus());
    }
}
