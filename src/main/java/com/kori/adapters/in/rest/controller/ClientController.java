package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.RestActorContextResolver;
import com.kori.adapters.in.rest.dto.Requests.UpdateStatusRequest;
import com.kori.adapters.in.rest.dto.Responses.UpdateStatusResponse;
import com.kori.application.command.UpdateClientStatusCommand;
import com.kori.application.port.in.UpdateClientStatusUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.CLIENTS)
@Tag(name = "Clients")
public class ClientController {

    private final UpdateClientStatusUseCase updateClientStatusUseCase;

    public ClientController(UpdateClientStatusUseCase updateClientStatusUseCase) {
        this.updateClientStatusUseCase = updateClientStatusUseCase;
    }

    @PatchMapping("/{clientId}/status")
    @Operation(summary = "Update client status")
    public UpdateStatusResponse updateClientStatus(
            @PathVariable String clientId,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = updateClientStatusUseCase.execute(
                new UpdateClientStatusCommand(actorContext, clientId, request.targetStatus(), request.reason())
        );
        return new UpdateStatusResponse(result.clientId(), result.previousStatus(), result.newStatus());
    }
}
