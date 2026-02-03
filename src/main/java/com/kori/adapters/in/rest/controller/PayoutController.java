package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.RestActorContextResolver;
import com.kori.adapters.in.rest.dto.Requests.FailPayoutRequest;
import com.kori.adapters.in.rest.dto.Requests.RequestAgentPayoutRequest;
import com.kori.adapters.in.rest.dto.Responses.AgentPayoutResponse;
import com.kori.application.command.CompleteAgentPayoutCommand;
import com.kori.application.command.FailAgentPayoutCommand;
import com.kori.application.command.RequestAgentPayoutCommand;
import com.kori.application.port.in.CompleteAgentPayoutUseCase;
import com.kori.application.port.in.FailAgentPayoutUseCase;
import com.kori.application.port.in.RequestAgentPayoutUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payouts")
public class PayoutController {

    private final RequestAgentPayoutUseCase requestAgentPayoutUseCase;
    private final CompleteAgentPayoutUseCase completeAgentPayoutUseCase;
    private final FailAgentPayoutUseCase failAgentPayoutUseCase;

    public PayoutController(RequestAgentPayoutUseCase requestAgentPayoutUseCase,
                            CompleteAgentPayoutUseCase completeAgentPayoutUseCase,
                            FailAgentPayoutUseCase failAgentPayoutUseCase) {
        this.requestAgentPayoutUseCase = requestAgentPayoutUseCase;
        this.completeAgentPayoutUseCase = completeAgentPayoutUseCase;
        this.failAgentPayoutUseCase = failAgentPayoutUseCase;
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public AgentPayoutResponse requestAgentPayout(
            @RequestHeader(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody RequestAgentPayoutRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = requestAgentPayoutUseCase.execute(
                new RequestAgentPayoutCommand(idempotencyKey, actorContext, request.agentCode())
        );
        return new AgentPayoutResponse(
                result.transactionId(),
                result.payoutId(),
                result.agentCode(),
                result.amount(),
                result.payoutStatus()
        );
    }

    @PostMapping("/{payoutId}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completePayout(
            @PathVariable String payoutId,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        completeAgentPayoutUseCase.execute(new CompleteAgentPayoutCommand(actorContext, payoutId));
    }

    @PostMapping("/{payoutId}/fail")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void failPayout(
            @PathVariable String payoutId,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody FailPayoutRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        failAgentPayoutUseCase.execute(new FailAgentPayoutCommand(actorContext, payoutId, request.reason()));
    }
}
