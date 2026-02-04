package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.RestActorContextResolver;
import com.kori.adapters.in.rest.doc.IdempotencyRequestHasher;
import com.kori.adapters.in.rest.doc.IdempotentOperation;
import com.kori.adapters.in.rest.dto.Requests.UpdateStatusRequest;
import com.kori.adapters.in.rest.dto.Responses.CreateAdminResponse;
import com.kori.adapters.in.rest.dto.Responses.UpdateStatusResponse;
import com.kori.application.command.CreateAdminCommand;
import com.kori.application.command.UpdateAdminStatusCommand;
import com.kori.application.port.in.CreateAdminUseCase;
import com.kori.application.port.in.UpdateAdminStatusUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.ADMINS)
@Tag(name = "Admins")
public class AdminController {

    private final CreateAdminUseCase createAdminUseCase;
    private final UpdateAdminStatusUseCase updateAdminStatusUseCase;
    private final IdempotencyRequestHasher idempotencyRequestHasher;

    public AdminController(CreateAdminUseCase createAdminUseCase, UpdateAdminStatusUseCase updateAdminStatusUseCase, IdempotencyRequestHasher idempotencyRequestHasher) {
        this.createAdminUseCase = createAdminUseCase;
        this.updateAdminStatusUseCase = updateAdminStatusUseCase;
        this.idempotencyRequestHasher = idempotencyRequestHasher;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create administrator")
    @IdempotentOperation
    public CreateAdminResponse createAdmin(
            @RequestHeader(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = createAdminUseCase.execute(
                new CreateAdminCommand(idempotencyKey, idempotencyRequestHasher.hashPayload(null), actorContext)
        );
        return new CreateAdminResponse(result.adminId());
    }

    @PatchMapping("/{adminId}/status")
    @Operation(summary = "Update admin status")
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
