package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.IdempotencyRequestHasher;
import com.kori.adapters.in.rest.RestActorContextResolver;
import com.kori.adapters.in.rest.dto.Requests.UpdateStatusRequest;
import com.kori.adapters.in.rest.dto.Responses.CreateAgentResponse;
import com.kori.adapters.in.rest.dto.Responses.UpdateStatusResponse;
import com.kori.application.command.CreateAgentCommand;
import com.kori.application.command.UpdateAgentStatusCommand;
import com.kori.application.port.in.CreateAgentUseCase;
import com.kori.application.port.in.UpdateAgentStatusUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.AGENTS)
public class AgentController {

    private final CreateAgentUseCase createAgentUseCase;
    private final UpdateAgentStatusUseCase updateAgentStatusUseCase;
    private final IdempotencyRequestHasher idempotencyRequestHasher;

    public AgentController(CreateAgentUseCase createAgentUseCase, UpdateAgentStatusUseCase updateAgentStatusUseCase, IdempotencyRequestHasher idempotencyRequestHasher) {
        this.createAgentUseCase = createAgentUseCase;
        this.updateAgentStatusUseCase = updateAgentStatusUseCase;
        this.idempotencyRequestHasher = idempotencyRequestHasher;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateAgentResponse createAgent(
            @RequestHeader(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = createAgentUseCase.execute(
                new CreateAgentCommand(idempotencyKey, idempotencyRequestHasher.hashPayload(null), actorContext)
        );
        return new CreateAgentResponse(result.agentId(), result.agentCode());
    }

    @PatchMapping("/{agentCode}/status")
    public UpdateStatusResponse updateAgentStatus(
            @PathVariable String agentCode,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = updateAgentStatusUseCase.execute(
                new UpdateAgentStatusCommand(actorContext, agentCode, request.targetStatus(), request.reason())
        );
        return new UpdateStatusResponse(result.agentCode(), result.previousStatus(), result.newStatus());
    }
}
