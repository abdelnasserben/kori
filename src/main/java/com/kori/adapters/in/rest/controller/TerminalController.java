package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.RestActorContextResolver;
import com.kori.adapters.in.rest.dto.Requests.CreateTerminalRequest;
import com.kori.adapters.in.rest.dto.Requests.UpdateStatusRequest;
import com.kori.adapters.in.rest.dto.Responses.CreateTerminalResponse;
import com.kori.adapters.in.rest.dto.Responses.UpdateStatusResponse;
import com.kori.application.command.CreateTerminalCommand;
import com.kori.application.command.UpdateTerminalStatusCommand;
import com.kori.application.port.in.CreateTerminalUseCase;
import com.kori.application.port.in.UpdateTerminalStatusUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.TERMINALS)
public class TerminalController {

    private final CreateTerminalUseCase createTerminalUseCase;
    private final UpdateTerminalStatusUseCase updateTerminalStatusUseCase;

    public TerminalController(CreateTerminalUseCase createTerminalUseCase,
                              UpdateTerminalStatusUseCase updateTerminalStatusUseCase) {
        this.createTerminalUseCase = createTerminalUseCase;
        this.updateTerminalStatusUseCase = updateTerminalStatusUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateTerminalResponse createTerminal(
            @RequestHeader(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody CreateTerminalRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = createTerminalUseCase.execute(
                new CreateTerminalCommand(idempotencyKey, actorContext, request.merchantCode())
        );
        return new CreateTerminalResponse(result.terminalId(), result.merchantCode());
    }

    @PatchMapping("/{terminalId}/status")
    public UpdateStatusResponse updateTerminalStatus(
            @PathVariable String terminalId,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = updateTerminalStatusUseCase.execute(
                new UpdateTerminalStatusCommand(actorContext, terminalId, request.targetStatus(), request.reason())
        );
        return new UpdateStatusResponse(result.terminalId(), result.previousStatus(), result.newStatus());
    }
}
