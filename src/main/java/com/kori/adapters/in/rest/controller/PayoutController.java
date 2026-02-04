package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.IdempotencyRequestHasher;
import com.kori.adapters.in.rest.IdempotentOperation;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.PAYOUTS)
@Tag(name = "Payouts")
public class PayoutController {

    private final RequestAgentPayoutUseCase requestAgentPayoutUseCase;
    private final CompleteAgentPayoutUseCase completeAgentPayoutUseCase;
    private final FailAgentPayoutUseCase failAgentPayoutUseCase;
    private final IdempotencyRequestHasher idempotencyRequestHasher;

    public PayoutController(RequestAgentPayoutUseCase requestAgentPayoutUseCase,
                            CompleteAgentPayoutUseCase completeAgentPayoutUseCase,
                            FailAgentPayoutUseCase failAgentPayoutUseCase, IdempotencyRequestHasher idempotencyRequestHasher) {
        this.requestAgentPayoutUseCase = requestAgentPayoutUseCase;
        this.completeAgentPayoutUseCase = completeAgentPayoutUseCase;
        this.failAgentPayoutUseCase = failAgentPayoutUseCase;
        this.idempotencyRequestHasher = idempotencyRequestHasher;
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Request agent payout")
    @IdempotentOperation
    public AgentPayoutResponse requestAgentPayout(
            @RequestHeader(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody RequestAgentPayoutRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = requestAgentPayoutUseCase.execute(
                new RequestAgentPayoutCommand(
                        idempotencyKey,
                        idempotencyRequestHasher.hashPayload(request),
                        actorContext,
                        request.agentCode()
                )
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
    @Operation(summary = "Complete payout")
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
    @Operation(summary = "Fail payout")
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
