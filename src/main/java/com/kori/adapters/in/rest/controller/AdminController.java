package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiHeaders;
import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.doc.IdempotencyRequestHasher;
import com.kori.adapters.in.rest.doc.IdempotentOperation;
import com.kori.adapters.in.rest.dto.Requests.CreateAdminRequest;
import com.kori.adapters.in.rest.dto.Requests.UpdateStatusRequest;
import com.kori.adapters.in.rest.dto.Responses.CreateAdminResponse;
import com.kori.adapters.in.rest.dto.Responses.UpdateStatusResponse;
import com.kori.application.command.CreateAdminCommand;
import com.kori.application.command.UpdateAdminStatusCommand;
import com.kori.application.port.in.CreateAdminUseCase;
import com.kori.application.port.in.UpdateAdminStatusUseCase;
import com.kori.application.security.ActorContext;
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
            @RequestHeader(ApiHeaders.IDEMPOTENCY_KEY) String idempotencyKey,
            ActorContext actorContext,
            @Valid @RequestBody CreateAdminRequest request
            ) {
        var result = createAdminUseCase.execute(
                new CreateAdminCommand(
                        idempotencyKey,
                        idempotencyRequestHasher.hashPayload(request),
                        actorContext,
                        request.username())
        );
        return new CreateAdminResponse(result.adminUsername());
    }

    @PatchMapping("/{adminUsername}/status")
    @Operation(summary = "Update admin status")
    public UpdateStatusResponse updateAdminStatus(
            @PathVariable String adminUsername,
            ActorContext actorContext,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        var result = updateAdminStatusUseCase.execute(
                new UpdateAdminStatusCommand(actorContext, adminUsername, request.targetStatus(), request.reason())
        );
        return new UpdateStatusResponse(result.adminUsername(), result.previousStatus(), result.newStatus());
    }
}
