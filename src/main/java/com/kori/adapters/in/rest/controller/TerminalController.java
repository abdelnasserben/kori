package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiHeaders;
import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.RestActorContextResolver;
import com.kori.adapters.in.rest.doc.IdempotencyRequestHasher;
import com.kori.adapters.in.rest.doc.IdempotentOperation;
import com.kori.adapters.in.rest.dto.Requests.CreateTerminalRequest;
import com.kori.adapters.in.rest.dto.Requests.UpdateStatusRequest;
import com.kori.adapters.in.rest.dto.Responses.CreateTerminalResponse;
import com.kori.adapters.in.rest.dto.Responses.UpdateStatusResponse;
import com.kori.application.command.CreateTerminalCommand;
import com.kori.application.command.UpdateTerminalStatusCommand;
import com.kori.application.port.in.CreateTerminalUseCase;
import com.kori.application.port.in.UpdateTerminalStatusUseCase;
import com.kori.application.security.ActorContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.TERMINALS)
@Tag(name = "Terminals")
public class TerminalController {

    private final CreateTerminalUseCase createTerminalUseCase;
    private final UpdateTerminalStatusUseCase updateTerminalStatusUseCase;
    private final IdempotencyRequestHasher idempotencyRequestHasher;

    public TerminalController(CreateTerminalUseCase createTerminalUseCase,
                              UpdateTerminalStatusUseCase updateTerminalStatusUseCase, IdempotencyRequestHasher idempotencyRequestHasher) {
        this.createTerminalUseCase = createTerminalUseCase;
        this.updateTerminalStatusUseCase = updateTerminalStatusUseCase;
        this.idempotencyRequestHasher = idempotencyRequestHasher;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create terminal")
    @IdempotentOperation
    public CreateTerminalResponse createTerminal(
            @RequestHeader(ApiHeaders.IDEMPOTENCY_KEY) String idempotencyKey,
            ActorContext actorContext,
            @Valid @RequestBody CreateTerminalRequest request
    ) {
        var result = createTerminalUseCase.execute(
                new CreateTerminalCommand(
                        idempotencyKey,
                        idempotencyRequestHasher.hashPayload(request),
                        actorContext,
                        request.merchantCode()
                )
        );
        return new CreateTerminalResponse(result.terminalId(), result.merchantCode());
    }

    @PatchMapping("/{terminalId}/status")
    @Operation(summary = "Update terminal status")
    public UpdateStatusResponse updateTerminalStatus(
            @PathVariable String terminalId,
            @Parameter(hidden = true) @RequestHeader(ApiHeaders.ACTOR_TYPE) String actorType,
            @Parameter(hidden = true) @RequestHeader(ApiHeaders.ACTOR_ID) String actorId,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = updateTerminalStatusUseCase.execute(
                new UpdateTerminalStatusCommand(actorContext, terminalId, request.targetStatus(), request.reason())
        );
        return new UpdateStatusResponse(result.terminalId(), result.previousStatus(), result.newStatus());
    }
}
